package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.CollectionUtil;
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
	private final List<ToRemap<Dependency>> inputs;
	private final List<Dependency> classpath;
	public boolean globalCache = false;
	private Dependency mappings;
	private String from, to;
	private List<ToRemap<File>> resolvedDependencies;
	private Iterable<File> resolvedMappings, resolvedClasspath;

	public RemappingDependency(Project project) {
		super(project, "io.github.amalgamation", null, "0.0.0");
		this.inputs = new ArrayList<>();
		this.classpath = new ArrayList<>();
	}

	public RemappingDependency(Project project,
			String group,
			String name,
			String version,
			Dependency mappings,
			String from,
			String to,
			List<ToRemap<Dependency>> inputs,
			List<Dependency> classpath) {
		super(project, group, name, version);
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
		if(this.mappings == null) {
			this.mappings = this.project.getDependencies().create(object);
			this.from = from;
			this.to = to;
			return this.mappings;
		}
		throw new IllegalStateException("cannot layer mappings like that yet");
	}

	public void remap(Object object, boolean useGlobalCache) {
		this.inputs.add(new ToRemap<>(this.project.getDependencies().create(object), useGlobalCache));
	}

	public void classpath(Object object) {
		this.classpath.add(this.project.getDependencies().create(object));
	}

	@Override
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		hasher.putUnencodedChars(this.from);
		hasher.putUnencodedChars(this.to);
		AmalgIO.hash(hasher, this.resolvedMappings);
		AmalgIO.hash(hasher, Iterables.transform(this.resolvedDependencies, ToRemap::dep));
		AmalgIO.hash(hasher, this.resolvedClasspath);
		return Base64.getUrlEncoder().encodeToString(hasher.hash().asBytes());
	}

	public List<ToRemap<File>> resolvePreserveConfig(Iterable<ToRemap<Dependency>> dependencies) {
		Iterable<Dependency> global, local;
		global = Iterables.transform(Iterables.filter(dependencies, d -> d.globalCache), ToRemap::dep);
		local = Iterables.transform(Iterables.filter(dependencies, d -> !d.globalCache), ToRemap::dep);
		Iterable<File> globalResolved = this.resolve(global), localResolved = this.resolve(local);
		List<ToRemap<File>> toRemaps = new ArrayList<>();
		globalResolved.forEach(f -> toRemaps.add(new ToRemap<>(f, true)));
		localResolved.forEach(f -> toRemaps.add(new ToRemap<>(f, false)));
		return toRemaps;
	}

	public Set<File> remap() throws IOException {
		Set<File> out = new HashSet<>();
		try(Clock ignored = new Clock("Remapped in %dms", this.project.getLogger())) {
			Hasher mappingsHasher = Hashing.sha256().newHasher();

			for(File mapping : this.resolvedMappings) {
				AmalgIO.hash(mappingsHasher, mapping);
			}
			byte[] mappingsHash = mappingsHasher.hash().asBytes();
			Map<ToRemap<File>, Path> toMap = new HashMap<>();

			List<Path> lastSecondClasspath = new ArrayList<>();
			for(ToRemap<File> dependency : this.resolvedDependencies) {
				Hasher hasher = Hashing.sha256().newHasher();
				AmalgIO.hash(hasher, dependency.dep);
				hasher.putBytes(mappingsHash);
				String unique = AmalgIO.hash(hasher);
				Path cached = AmalgIO.cache(this.project, dependency.globalCache).resolve("remaps").resolve(unique+"_"+dependency.dep.getName());
				if(!Files.exists(cached)) {
					toMap.put(dependency, cached);
				} else {
					lastSecondClasspath.add(cached);
				}
				out.add(cached.toFile());
			}

			this.project.getLogger().lifecycle("Remapping " + toMap.size() + " dependencies");
			if(toMap.isEmpty()) {
				return out;
			}
			MappingSet mappings = MappingSet.create();
			for(File mapping : this.resolvedMappings) {
				MappingUtil.loadMappings(mappings, mapping, this.from, this.to);
			}

			TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(MappingUtil.createMappingProvider(mappings)).build();
			lastSecondClasspath.forEach(remapper::readClassPathAsync);
			Map<ToRemap<File>, InputTag> tags = new HashMap<>();
			for(ToRemap<File> file : toMap.keySet()) {
				InputTag tag = remapper.createInputTag();
				tags.put(file, tag);
				remapper.readInputsAsync(tag, file.dep.toPath());
			}

			for(File file : RemappingDependency.this.resolvedClasspath) {
				remapper.readClassPathAsync(file.toPath());
			}

			for(Map.Entry<ToRemap<File>, InputTag> entry : tags.entrySet()) {
				InputTag tag = entry.getValue();
				File file = entry.getKey().dep;
				Path destination = toMap.get(entry.getKey());
				try(OutputConsumerPath output = new OutputConsumerPath.Builder(destination).build()) {
					output.addNonClassFiles(file.toPath(), NonClassCopyMode.FIX_META_INF, remapper);
					remapper.apply(output, tag);
				}
			}

			remapper.finish();
		}
		return out;
	}

	@Override
	protected Set<File> path() throws IOException {
		if(this.resolved == null) {
			Configuration configuration = this.project.getConfigurations().detachedConfiguration(RemappingDependency.this.mappings);
			this.resolvedMappings = configuration.resolve();
			List<File> resources = new ArrayList<>();

			List<ToRemap<File>> toRemap = this.resolvePreserveConfig(this.inputs);
			Iterator<ToRemap<File>> iterator = toRemap.iterator();
			while(iterator.hasNext()) {
				ToRemap<File> file = iterator.next();
				if(AmalgIO.isResourcesJar(file.dep)) {
					resources.add(file.dep);
					iterator.remove();
				}
			}

			this.resolvedDependencies = toRemap;
			//CollectionUtil.filt(this.resolve(this.classpath), resources, AmalgIO::isResourcesJar);
			this.resolvedClasspath = Iterables.filter(this.resolve(this.classpath), AmalgIO::jarContainsClasses);
			Set<File> output = this.remap();
			output.addAll(resources);
			this.resolved = output;
		}
		return this.resolved;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		throw new IllegalStateException("wat");
	}

	@Override
	public Dependency copy() {
		return new RemappingDependency(
				this.project,
				this.group,
				this.name,
				this.version,
				this.mappings,
				this.from,
				this.to,
				new ArrayList<>(this.inputs),
				new ArrayList<>(this.classpath));
	}

	static class ToRemap<T> {
		final T dep;
		final boolean globalCache;

		ToRemap(T dependency, boolean cache) {
			this.dep = dependency;
			this.globalCache = cache;
		}

		public T dep() {
			return this.dep;
		}
	}
}
