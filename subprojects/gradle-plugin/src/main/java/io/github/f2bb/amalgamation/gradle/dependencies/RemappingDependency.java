package io.github.f2bb.amalgamation.gradle.dependencies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.gradle.util.Mappings;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemappingDependency extends AbstractSelfResolvingDependency {
	public final CachedFile<?> remapper;
	private final List<MappingsDependency> mappings;
	private final List<Dependency> inputs, classpath;

	public RemappingDependency(Project project) {
		super(project, "io.github.amalgamation", null, "0.0.0");
		this.mappings = new ArrayList<>();
		this.inputs = new ArrayList<>();
		this.classpath = new ArrayList<>();
		this.remapper = new CachedFile<Void>(() -> CachedFile.globalCache(this.project.getGradle()).resolve(this.getName()), Void.class) {
			@Nullable
			@Override
			protected Void writeFile(Path path, @Nullable Void currentData) throws IOException {
				if (Files.exists(path)) {
					return null;
				}

				MappingSet mappings = MappingSet.create();
				for (MappingsDependency mapping : RemappingDependency.this.mappings) {
					Configuration configuration = project.getConfigurations().detachedConfiguration(mapping.dependency);
					loadMappings(mappings, configuration.resolve(), mapping.from, mapping.to);
				}

				TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(Mappings.createMappingProvider(mappings)).build();

				Map<File, InputTag> tags = new HashMap<>();
				for (File file : RemappingDependency.this.resolve(RemappingDependency.this.inputs)) {
					InputTag tag = remapper.createInputTag();
					tags.put(file, tag);
					remapper.readInputsAsync(tag, file.toPath());
				}

				for (File file : RemappingDependency.this.resolve(RemappingDependency.this.classpath)) {
					remapper.readClassPathAsync(file.toPath());
				}

				for (Map.Entry<File, InputTag> entry : tags.entrySet()) {
					InputTag tag = entry.getValue();
					Path destination = path.resolve(Hashing.sha256().hashUnencodedChars(entry.getKey().getAbsolutePath()).toString() + ".jar");
					try (OutputConsumerPath output = new OutputConsumerPath.Builder(destination).build()) {
						output.addNonClassFiles(entry.getKey().toPath(), NonClassCopyMode.FIX_META_INF, remapper);
						remapper.apply(output, tag);
					}
				}

				remapper.finish();
				return null;
			}
		};
	}

	private static void loadMappings(MappingSet mappings, Set<File> files, String from, String to) throws IOException {
		for (File file : files) {
			try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(),
					null); BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
				new TinyMappingsReader(TinyMappingFactory.loadWithDetection(reader), from, to).read(mappings);
			}
		}
	}

	public RemappingDependency(Project project,
			String group,
			String name,
			String version,
			CachedFile<?> remapper,
			List<MappingsDependency> mappings,
			List<Dependency> inputs,
			List<Dependency> classpath) {
		super(project, group, name, version);
		this.remapper = remapper;
		this.mappings = mappings;
		this.inputs = inputs;
		this.classpath = classpath;
	}

	/**
	 * this layers mappings only, it does not map each mapping one after the other!
	 *
	 * @param object the dependency
	 * @param from the origin namespace
	 * @param to the destination namespace
	 */
	public void mappings(Object object, String from, String to) {
		this.mappings.add(new MappingsDependency(this.project.getDependencies().create(object), from, to));
	}

	public void remap(Object object) {
		this.inputs.add(this.project.getDependencies().create(object));
	}

	public void classpath(Object object) {
		this.inputs.add(this.project.getDependencies().create(object));
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		return () -> {
			try {
				return Files.walk(this.remapper.getPath()).filter(Files::isRegularFile).iterator();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
	}

	@Override
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		Configuration configuration = this.project.getConfigurations().detachedConfiguration();
		for (MappingsDependency mapping : this.mappings) {
			hasher.putUnencodedChars(mapping.from);
			hasher.putUnencodedChars(mapping.to);
			configuration.getDependencies().add(mapping.dependency);
		}
		hash(hasher, configuration.resolve());

		hash(hasher, this.resolve(this.inputs));
		hash(hasher, this.resolve(this.classpath));
		return hasher.hash().toString();
	}

	@Override
	public Dependency copy() {
		return new RemappingDependency(this.project,
				this.group,
				this.name,
				this.version,
				this.remapper,
				new ArrayList<>(this.mappings),
				new ArrayList<>(this.inputs),
				new ArrayList<>(this.classpath));
	}

	static class MappingsDependency {
		final Dependency dependency;
		final String from, to;

		MappingsDependency(Dependency dependency, String from, String to) {
			this.dependency = dependency;
			this.from = from;
			this.to = to;
		}
	}
}
