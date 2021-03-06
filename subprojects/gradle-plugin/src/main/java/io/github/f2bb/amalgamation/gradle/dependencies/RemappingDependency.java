package io.github.f2bb.amalgamation.gradle.dependencies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.gradle.util.Clock;
import io.github.f2bb.amalgamation.gradle.util.LazySet;
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
	private final List<Dependency> inputs, classpath;
	private Dependency mappings;
	private String from, to;

	public RemappingDependency(Project project) {
		super(project, "io.github.amalgamation", null, "0.0.0");
		this.inputs = new ArrayList<>();
		this.classpath = new ArrayList<>();
		this.remapper = new CachedFile<Void>(() -> CachedFile.globalCache(this.project.getGradle()).resolve(this.getName()), Void.class) {
			@Nullable
			@Override
			protected Void writeFile(Path path, @Nullable Void currentData) throws IOException {
				if (Files.exists(path)) {
					return null;
				}

				project.getLogger().lifecycle("Remapping " + inputs.size() + " dependencies");
				Clock clock = new Clock("Remapped " + RemappingDependency.this.inputs.size() + " dependencies in %dms", project.getLogger());
				MappingSet mappings = MappingSet.create();

				loadMappings(mappings, RemappingDependency.this.resolvedMappings, RemappingDependency.this.from, RemappingDependency.this.to);

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
				clock.end();
				return null;
			}
		};
	}

	private static void loadMappings(MappingSet mappings, Iterable<File> files, String from, String to) throws IOException {
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
			Dependency mappings,
			String from,
			String to,
			List<Dependency> inputs,
			List<Dependency> classpath) {
		super(project, group, name, version);
		this.remapper = remapper;
		this.mappings = mappings;
		this.from = from;
		this.to = to;
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
		if (this.mappings == null) {
			this.mappings = this.project.getDependencies().create(object);
			this.from = from;
			this.to = to;
			return;
		}
		throw new IllegalStateException("cannot layer mappings like that yet");
	}

	public void remap(Object object) {
		this.inputs.add(this.project.getDependencies().create(object));
	}

	public void classpath(Object object) {
		this.inputs.add(this.project.getDependencies().create(object));
	}

	private Iterable<File> resolvedMappings, resolvedDependencies, resolvedClasspath;
	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			Configuration configuration = this.project.getConfigurations().detachedConfiguration(RemappingDependency.this.mappings);
			this.resolvedMappings = configuration.resolve();
			this.resolvedDependencies = this.resolve(this.inputs);
			this.resolvedClasspath = this.resolve(this.classpath);

			this.resolved = new LazySet(CompletableFuture.supplyAsync(() -> {
				try {
					return Files.walk(this.remapper.getPath()).filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toSet());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
		}
		return this.resolved;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		throw new IllegalStateException("wat");
	}

	@Override
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		hasher.putUnencodedChars(this.from);
		hasher.putUnencodedChars(this.to);
		hash(hasher, this.resolvedMappings);
		hash(hasher, this.resolvedDependencies);
		hash(hasher, this.resolvedClasspath);

		return hasher.hash().toString();
	}

	@Override
	public Dependency copy() {
		return new RemappingDependency(this.project,
				this.group,
				this.name,
				this.version,
				this.remapper,
				this.mappings,
				this.from,
				this.to,
				new ArrayList<>(this.inputs),
				new ArrayList<>(this.classpath));
	}
}
