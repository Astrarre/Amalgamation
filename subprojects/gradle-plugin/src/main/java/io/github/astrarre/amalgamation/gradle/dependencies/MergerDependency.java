package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.FileUtil;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

public class MergerDependency extends AbstractSelfResolvingDependency {
	public final CachedFile<?> merger;
	private final Map<List<TypeEntry>, Collection<Dependency>> unique = new HashMap<>(), merge = new HashMap<>();
	private final List<TypeEntry> additionalEntries = new ArrayList<>();
	private final Map<List<TypeEntry>, Iterable<File>> uniqueResolved = new HashMap<>(), mergeResolved = new HashMap<>();
	public boolean compareInstructions = true, leaveMarker = true, globalCache = false;

	public MergerDependency(Project project) {
		super(project, "io.github.f2bb", null, "0.0.0");
		this.merger = new MergerCacheFile(this, project);
	}

	public static String toString(Dependency dependency) {
		return dependency.getGroup() + ':' + dependency.getName() + ':' + dependency.getVersion() + ':' + dependency.getReason() + ':' + dependency.getClass();
	}

	@Override
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		this.mergeResolved.forEach((strings, dependencies) -> {
			for (TypeEntry string : strings) {
				hasher.putUnencodedChars(string.type);
				hasher.putUnencodedChars(string.entry);
			}
			FileUtil.hash(hasher, dependencies);
		});
		this.uniqueResolved.forEach((strings, dependencies) -> {
			for (TypeEntry string : strings) {
				hasher.putUnencodedChars(string.type);
				hasher.putUnencodedChars(string.entry);
			}
			FileUtil.hash(hasher, dependencies);
		});

		hasher.putBoolean(this.compareInstructions);
		return hasher.hash().toString();
	}

	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			this.unique.forEach((strings, dependencies) -> this.uniqueResolved.put(strings, this.resolve(dependencies)));
			this.merge.forEach((strings, dependencies) -> this.mergeResolved.put(strings, this.resolve(dependencies)));
			Path path = this.merger.getPath(); // order matters
			Set<File> files = new HashSet<>();
			try {
				Files.walk(path).filter(Files::isRegularFile).forEach(path1 -> files.add(path1.toFile()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.resolved = files;
			return files;
		}
		return this.resolved;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		throw new UnsupportedOperationException();
	}

	public void include(Object object, TypeEntry... platforms) {
		this.merge.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	public void addUnique(Object object, TypeEntry... platforms) {
		this.unique.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	@Override
	public Dependency copy() {
		return new MergerDependency(this.project);
	}

	public void appendEntry(String type, String entry) {
		this.additionalEntries.add(this.of(type, entry));
	}

	public TypeEntry of(String type, String entry) {
		return new TypeEntry(type, entry);
	}

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

	private static class MergerCacheFile extends CachedFile<Void> {
		private final Project project;
		private final MergerDependency dep;

		public MergerCacheFile(MergerDependency dependency, Project project) {
			super(() -> (dependency.globalCache ? FileUtil.globalCache(dependency.project.getGradle()) :
			             FileUtil.projectCache(dependency.project)).resolve("merges").resolve(dependency.getName()), Void.class);
			this.dep = dependency;
			this.project = project;
		}

		@Nullable
		@Override
		protected Void writeIfOutdated(Path path, @Nullable Void currentData) throws Throwable {
			if (Files.exists(path)) {
				return null;
			}
			this.project.getLogger().lifecycle("Merging " + (this.dep.unique.size() + this.dep.merge.size()) + " dependencies");
			// todo instead put metadata in the files so no duplication
			try (Clock ignored = new Clock("Merged " + (this.dep.unique.size() + this.dep.merge.size()) + " dependencies " + "in %dms",
					this.project.getLogger())) {
				// todo seperate out unique files or just optimize it
				Map<String, Object> config = new HashMap<>();
				config.put("compareInstructions", this.dep.compareInstructions);
				Map<String, List<String>> typeEntries = new HashMap<>();
				for (TypeEntry entry : this.dep.additionalEntries) {
					typeEntries.computeIfAbsent(entry.type, s -> new ArrayList<>()).add(entry.entry);
				}
				Map<List<String>, Iterable<File>> contextMap = new HashMap<>();
				for (Map.Entry<List<TypeEntry>, Iterable<File>> entry : Iterables.concat(this.dep.uniqueResolved.entrySet(),
						this.dep.mergeResolved.entrySet())) {
					contextMap.put(Lists.transform(entry.getKey(), t -> t.entry), entry.getValue());
				}
				Files.createDirectories(path);
				MergeUtil.merge(typeEntries,
						MergeUtil.defaults(config),
						path.resolve("merged.jar"),
						contextMap,
						strings -> CachedFile.forHash(path, sink -> strings.forEach(sink::putUnencodedChars)).resolve("resources.jar"),
						this.dep.leaveMarker);
			}
			return null;
		}
	}
}
