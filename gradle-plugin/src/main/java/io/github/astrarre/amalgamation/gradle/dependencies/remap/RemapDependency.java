package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.NamespacedMappingsDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AbstractRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.bin.TRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.src.TrieHarderRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import net.devtech.zipio.processors.zip.PostZipProcessor;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemapDependency extends ZipProcessDependency {
	private final List<Dependency> inputsLocal = new ArrayList<>(), inputsGlobal = new ArrayList<>();

	private final List<Dependency> classpath = new ArrayList<>();
	private final List<NamespacedMappingsDependency> mappings = new ArrayList<>();
	private AmalgRemapper classRemapper = new TRemapper();

	public RemapDependency(Project project) {
		super(project, "io.github.astrarre.amalgamation", "remapped", "1.0.0");
	}

	/**
	 * this layers mappings only, it does not maps each mapping one after the other!
	 *
	 * @param object the dependency
	 * @param from the origin namespace
	 * @param to the destination namespace
	 */
	public Dependency mappings(Object object, String from, String to) {
		NamespacedMappingsDependency mappings = new NamespacedMappingsDependency(this.project, this.of(object), from, to);
		this.mappings.add(mappings);
		return mappings;
	}

	public void remapper(AmalgRemapper remapper) {
		this.classRemapper = remapper;
	}

	public void remapper(AbstractRemapper remapper, AmalgRemapper sourceRemapper) {
		this.classRemapper = remapper;
		remapper.setSourceRemapper(sourceRemapper);
	}

	public AbstractRemapper tinyRemapper() {
		if(this.classRemapper instanceof TRemapper t) {
			return t;
		} else {
			TRemapper remapper = new TRemapper();
			this.classRemapper = remapper;
			return remapper;
		}
	}

	public void trieHarder() {
		if(this.classRemapper instanceof AbstractRemapper a) {
			a.setSourceRemapper(new TrieHarderRemapper());
		} else {
			throw new UnsupportedOperationException("cannot set source remapper on non-AbstractRemapper remapper!");
		}
	}

	public void mappings(NamespacedMappingsDependency dependency) {
		this.mappings.add(dependency);
	}

	public void remap(Object object, boolean useGlobalCache) {
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(this.project.getDependencies().create(object));
	}

	public void remap(Object object, boolean useGlobalCache, Closure<ModuleDependency> config) {
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(this.project.getDependencies().create(object, config));
	}

	public void classpath(Object object) {
		this.classpath.add(this.project.getDependencies().create(object));
	}

	public void classpath(Object object, Closure<ModuleDependency> config) {
		this.classpath.add(this.project.getDependencies().create(object, config));
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		for(Dependency dependency : this.classpath) {
			this.hashDep(hasher, dependency);
		}

		for(Dependency dependency : this.inputsGlobal) {
			this.hashDep(hasher, dependency);
		}

		for(Dependency dependency : this.inputsLocal) {
			this.hashDep(hasher, dependency);
		}

		this.hashMappings(hasher);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return AmalgIO.cache(this.project, this.inputsLocal.isEmpty()).resolve("remaps").resolve(AmalgIO.b64(hash));
	}

	private void hashMappings(Hasher hasher) throws IOException {
		for(var mapping : this.mappings) {
			this.hashDep(hasher, mapping.forward());
			hasher.putString(mapping.from(), StandardCharsets.UTF_8);
			hasher.putString(mapping.to(), StandardCharsets.UTF_8);
		}
	}

	@Override
	protected void add(ZipProcessBuilder builder, Path resolvedPath, boolean isOutdated) throws IOException {
		Hasher hasher = HASHING.newHasher();
		this.hashMappings(hasher);
		byte[] mappingsHash = hasher.hash().asBytes();

		AmalgRemapper remapper = this.classRemapper;
		if(isOutdated) {
			List<Mappings.Namespaced> maps = new ArrayList<>();
			for(var mapping : this.mappings) {
				maps.add(mapping.read());
			}

			this.classRemapper.init(maps);
		}

		this.extracted(builder, isOutdated, mappingsHash, remapper, this.inputsGlobal, true, isOutdated);
		this.extracted(builder, isOutdated, mappingsHash, remapper, this.inputsLocal, false, isOutdated);
		this.extracted(builder, isOutdated, mappingsHash, remapper, this.classpath, false, true);
	}

	private void extracted(ZipProcessBuilder builder,
			boolean isOutdated,
			byte[] mappingsHash,
			AmalgRemapper remapper,
			List<Dependency> deps,
			boolean global,
			boolean isClasspath) throws IOException {
		for(Dependency dependency : deps) {
			var dep = new SingleRemapDependency(this.project, remapper, mappingsHash, dependency, global, isClasspath);
			dep.add(builder, dep.getPath(), isOutdated && dep.isOutdated());
		}
	}
}
