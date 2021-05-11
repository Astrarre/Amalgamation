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
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.f2bb.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.gradle.util.Clock;
import io.github.f2bb.amalgamation.gradle.util.LazySet;
import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.merger.PlatformMerger;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

public class MergerDependency extends AbstractSingleFileSelfResolvingDependency {
	public static final Map<String, ?> CREATE_ZIP = ImmutableMap.of("create", "true");
	public final CachedFile<?> merger;
	private final Map<List<String>, Collection<Dependency>> unique = new HashMap<>(), merge = new HashMap<>();
	public boolean compareInstructions = true;

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
				project.getLogger().lifecycle("Merging " + (unique.size() + merge.size()) + " dependencies");

				try (Clock ignored = new Clock("Merged " + (unique.size() + merge.size()) + " dependencies in %dms", project.getLogger())) {
					// todo seperate out unique files or just optimize it
					Map<String, Object> map = new HashMap<>();
					map.put("compareInstructions", MergerDependency.this.compareInstructions);
					LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project);
					Collection<PlatformData> data = new ArrayList<>();
					try {
						for (Map.Entry<List<String>, Iterable<File>> entry : Iterables.concat(uniqueResolved.entrySet(), mergeResolved.entrySet())) {
							List<String> names = entry.getKey();
							Iterable<File> dependency = entry.getValue();

							PlatformData platform = new PlatformData(names, new ArrayList<>());
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
							PlatformMerger.merge(meta.createContext(system.getRootDirectories()), data, map);
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
			strings.forEach(hasher::putUnencodedChars);
			hash(hasher, dependencies);
		});
		this.uniqueResolved.forEach((strings, dependencies) -> {
			strings.forEach(hasher::putUnencodedChars);
			hash(hasher, dependencies);
		});

		hasher.putBoolean(this.compareInstructions);
		return hasher.hash().toString();
	}

	private final Map<List<String>, Iterable<File>> uniqueResolved = new HashMap<>(), mergeResolved = new HashMap<>();
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

	public void include(Object object, String... platforms) {
		this.merge.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	public void addUnique(Object object, String... platforms) {
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
}
