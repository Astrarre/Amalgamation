package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.CollectionUtil;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.MappingUtil;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemappingDependency extends AbstractSelfResolvingDependency {
	public final CachedFile<?> remapper;
	public boolean globalCache = false;
	private final List<Dependency> inputs, classpath;
	private Dependency mappings;
	private String from, to;

	public RemappingDependency(Project project) {
		super(project, "io.github.amalgamation", null, "0.0.0");
		this.inputs = new ArrayList<>();
		this.classpath = new ArrayList<>();
		this.remapper = new CachedFile<Void>(() -> AmalgIO.cache(project, this.globalCache).resolve("remaps").resolve(this.getName()), Void.class) {
			@Nullable
			@Override
			protected Void writeIfOutdated(Path path, @Nullable Void currentData) throws IOException {
				if (Files.exists(path)) {
					return null;
				}

				project.getLogger().lifecycle("Remapping " + RemappingDependency.this.inputs.size() + " dependencies");

				try (Clock ignored = new Clock("Remapped " + RemappingDependency.this.inputs.size() + " dependencies in %dms", project.getLogger())) {
					MappingSet mappings = MappingSet.create();

					for (File mapping : RemappingDependency.this.resolvedMappings) {
						MappingUtil.loadMappings(mappings, mapping, RemappingDependency.this.from, RemappingDependency.this.to);
					}

					TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(MappingUtil.createMappingProvider(mappings)).build();

					Map<File, InputTag> tags = new HashMap<>();
					for (File file : RemappingDependency.this.resolvedDependencies) {
						InputTag tag = remapper.createInputTag();
						tags.put(file, tag);
						remapper.readInputsAsync(tag, file.toPath());
					}

					for (File file : RemappingDependency.this.resolvedClasspath) {
						remapper.readClassPathAsync(file.toPath());
					}

					int id = 0;
					for (Map.Entry<File, InputTag> entry : tags.entrySet()) {
						InputTag tag = entry.getValue();
						File file = entry.getKey();
						Path destination = path.resolve("&" + (id++) + ".jar");
						try (OutputConsumerPath output = new OutputConsumerPath.Builder(destination).build()) {
							output.addNonClassFiles(file.toPath(), NonClassCopyMode.FIX_META_INF, remapper);
							remapper.apply(output, tag);
						}
					}

					remapper.finish();
				}

				return null;
			}
		};
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
	public Dependency mappings(Object object, String from, String to) {
		if (this.mappings == null) {
			this.mappings = this.project.getDependencies().create(object);
			this.from = from;
			this.to = to;
			return this.mappings;
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
	protected Set<File> path() throws IOException {
		if (this.resolved == null) {
			Configuration configuration = this.project.getConfigurations().detachedConfiguration(RemappingDependency.this.mappings);
			this.resolvedMappings = configuration.resolve();
			List<File> resources = new ArrayList<>();
			this.resolvedDependencies = CollectionUtil.filt(this.resolve(this.inputs), resources, AmalgIO::isResourcesJar);
			this.resolvedClasspath = CollectionUtil.filt(this.resolve(this.classpath), resources, AmalgIO::isResourcesJar);
			Set<File> files = Files.walk(this.remapper.getPath())
			                       .filter(Files::isRegularFile)
			                       .map(Path::toFile)
			                       .collect(Collectors.toCollection(HashSet::new));
			files.addAll(resources);
			this.resolved = files;
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
		AmalgIO.hash(hasher, this.resolvedMappings);
		AmalgIO.hash(hasher, this.resolvedDependencies);
		AmalgIO.hash(hasher, this.resolvedClasspath);
		return Base64.getUrlEncoder().encodeToString(hasher.hash().asBytes());
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
