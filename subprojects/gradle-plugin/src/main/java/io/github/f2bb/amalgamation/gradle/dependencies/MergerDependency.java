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
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.merger.PlatformMerger;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

public class MergerDependency extends AbstractSelfResolvingDependency {
	public static final Map<String, ?> CREATE_ZIP = ImmutableMap.of("create", "true");
	public final CachedFile<?> merger;
	private final Map<Collection<String>, Collection<Dependency>> unique = new HashMap<>(), merge = new HashMap<>();
	public boolean compareInstructions = true;

	public MergerDependency(Project project) {
		super(project, "io.github.f2bb", null, "0.0.0");
		this.merger = new CachedFile<Void>(() -> CachedFile.globalCache(this.project.getGradle()).resolve(this.getName()).resolve("merged.jar"),
				Void.class) {
			@Nullable
			@Override
			protected Void writeFile(Path path, @Nullable Void currentData) throws Throwable {
				if (!Files.exists(path)) {
					// todo seperate out unique files or just optimize it
					LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project);
					Collection<PlatformData> data = new ArrayList<>();
					for (Map.Entry<Collection<String>, Collection<Dependency>> entry : Iterables.concat(unique.entrySet(), merge.entrySet())) {
						Collection<String> names = entry.getKey();
						Collection<Dependency> dependency = entry.getValue();
						Map<String, byte[]> map = new HashMap<>();
						for (File file : MergerDependency.this.resolve(dependency)) {
							try (FileSystem system = FileSystems.newFileSystem(file.toPath(), null)) {
								for (Path directory : system.getRootDirectories()) {
									PlatformData.readFiles(map, directory);
								}
							}
						}
						data.add(new PlatformData(names, map));
					}


					Files.createDirectories(path.getParent());
					try (FileSystem system = FileSystems.newFileSystem(new URI("jar:" + path.toUri()), CREATE_ZIP)) {
						PlatformMerger.merge(meta.createContext(system.getRootDirectories()), data);
					}
				}
				return null;
			}
		};
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
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		hash(hasher, this.resolve(Iterables.concat(this.unique.values())));
		hash(hasher, this.resolve(Iterables.concat(this.merge.values())));
		hasher.putBoolean(this.compareInstructions);
		return hasher.hash().toString();
	}

	@Override
	public Dependency copy() {
		return new MergerDependency(this.project);
	}

	private String toString(Dependency dependency) {
		return dependency.getGroup() + ':' + dependency.getName() + ':' + dependency.getVersion();
	}
}
