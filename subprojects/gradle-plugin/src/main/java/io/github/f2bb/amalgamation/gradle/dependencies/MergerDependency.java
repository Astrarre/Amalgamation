package io.github.f2bb.amalgamation.gradle.dependencies;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.gradle.util.Clock;
import io.github.f2bb.amalgamation.gradle.util.LazySet;
import io.github.f2bb.amalgamation.platform.merger.DefaultMergeContext;
import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.merger.PlatformMerger;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

public class MergerDependency extends AbstractSingleFileSelfResolvingDependency {
	public static final Map<String, ?> CREATE_ZIP = ImmutableMap.of("create", "true");
	public final CachedFile<?> merger;
	private final Map<List<TypeEntry>, Collection<Dependency>> unique = new HashMap<>(), merge = new HashMap<>();
	public boolean compareInstructions = true;
	private final List<TypeEntry> additionalEntries = new ArrayList<>();

	public MergerDependency(Project project) {
		super(project, "io.github.f2bb", null, "0.0.0");
		this.merger = new CachedFile<Void>(() -> CachedFile.globalCache(this.project.getGradle()).resolve(this.getName()).resolve("merged.jar"),
				Void.class) {
			@Nullable
			@Override
			protected Void writeIfOutdated(Path path, @Nullable Void currentData) throws Throwable {
				if (Files.exists(path)) {
					return null;
				}
				project.getLogger().lifecycle("Merging " + (MergerDependency.this.unique.size() + MergerDependency.this.merge.size()) + " dependencies");

				try (Clock ignored = new Clock("Merged " + (MergerDependency.this.unique.size() + MergerDependency.this.merge.size()) + " dependencies in %dms", project.getLogger())) {
					// todo seperate out unique files or just optimize it
					Map<String, Object> config = new HashMap<>();
					config.put("compareInstructions", MergerDependency.this.compareInstructions);
					Collection<PlatformData> data = new ArrayList<>();
					try {
						Map<String, List<String>> typeEntries = new HashMap<>();
						for (TypeEntry entry : MergerDependency.this.additionalEntries) {
							typeEntries.computeIfAbsent(entry.type, s -> new ArrayList<>()).add(entry.entry);
						}
						for (Map.Entry<List<TypeEntry>, Iterable<File>> entry : Iterables.concat(
								MergerDependency.this.uniqueResolved.entrySet(),
								MergerDependency.this.mergeResolved.entrySet())) {
							List<TypeEntry> names = entry.getKey();
							for (TypeEntry name : names) {
								typeEntries.computeIfAbsent(name.type, s -> new ArrayList<>()).add(name.entry);
							}
							Iterable<File> dependency = entry.getValue();

							List<String> str = names.stream().map(entry1 -> entry1.entry).collect(Collectors.toList());
							PlatformData platform = new PlatformData(str, new ArrayList<>());
							for (File file : dependency) {
								FileSystem system = FileSystems.newFileSystem(file.toPath(), null);
								for (Path directory : system.getRootDirectories()) {
									platform.paths.add(directory);
								}
								platform.addCloseAction(system);
							}
							data.add(platform);
						}


						Files.createDirectories(path.getParent());

						try (FileSystem system = FileSystems.newFileSystem(new URI("jar:" + path.toUri()), CREATE_ZIP)) {
							DefaultMergeContext mergeContext = new DefaultMergeContext(system.getRootDirectories(), typeEntries);
							PlatformMerger.merge(mergeContext, data, config);
						}

					} finally {
						for (PlatformData datum : data) {
							datum.close();
						}
					}
				}

				return null;
			}
		};
	}

	@Override
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		this.mergeResolved.forEach((strings, dependencies) -> {
			for (TypeEntry string : strings) {
				hasher.putUnencodedChars(string.type);
				hasher.putUnencodedChars(string.entry);
			}
			hash(hasher, dependencies);
		});
		this.uniqueResolved.forEach((strings, dependencies) -> {
			for (TypeEntry string : strings) {
				hasher.putUnencodedChars(string.type);
				hasher.putUnencodedChars(string.entry);
			}
			hash(hasher, dependencies);
		});

		hasher.putBoolean(this.compareInstructions);
		return hasher.hash().toString();
	}

	private final Map<List<TypeEntry>, Iterable<File>> uniqueResolved = new HashMap<>(), mergeResolved = new HashMap<>();
	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			this.unique.forEach((strings, dependencies) -> this.uniqueResolved.put(strings, this.resolve(dependencies)));
			this.merge.forEach((strings, dependencies) -> this.mergeResolved.put(strings, this.resolve(dependencies)));
			this.resolved = new LazySet(CompletableFuture.supplyAsync(() -> Collections.singleton(this.merger.getPath().toFile()),
					BaseAmalgamationImpl.SERVICE));
		}
		return this.resolved;
	}

	public void include(Object object, TypeEntry... platforms) {
		this.merge.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	public void addUnique(Object object, TypeEntry... platforms) {
		this.unique.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	@Override
	protected Path resolvePath() {
		return this.merger.getPath();
	}

	@Override
	public Dependency copy() {
		return new MergerDependency(this.project);
	}

	public static String toString(Dependency dependency) {
		return dependency.getGroup() + ':' + dependency.getName() + ':' + dependency.getVersion() + ':' + dependency.getReason() + ':' + dependency.getClass();
	}


	public void appendEntry(String type, String entry) {
		this.additionalEntries.add(of(type, entry));
	}

	/**
	 * a 'type' and it's value, aids in merging. For example, the entry for '1.16.5' would be 'version', '1.16.5'.
	 *
	 * These assist in annotation merging, eg. `@Platform("fabric", "client"), @Platform("fabric", "server")` == `@Platform("fabric")`
	 */
	public TypeEntry of(String type, String entry) {
		return new TypeEntry(type, entry);
	}

	/**
	 * if the entry belongs to it's own thing
	 */
	public TypeEntry ofRand(String entry) {
		return new TypeEntry(UUID.randomUUID().toString(), entry);
	}

	/**
	 * a 'type' and it's value, aids in merging. For example, the entry for '1.16.5' would be 'version', '1.16.5'
	 */
	public static final class TypeEntry {
		public final String type, entry;

		public TypeEntry(String type, String entry) {
			this.type = type;
			this.entry = entry;
		}

		@Override
		public String toString() {
			return this.type + " of " + this.entry;
		}
	}
}
