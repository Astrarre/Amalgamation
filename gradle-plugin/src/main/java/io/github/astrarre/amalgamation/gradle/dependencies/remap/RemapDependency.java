package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class RemapDependency extends ZipProcessDependency {
	final DependencyRemapConfig config;

	// todo add resource remappers

	public RemapDependency(Project project, DependencyRemapConfig config) {
		super(project, "io.github.astrarre.amalgamation", "remapped", "1.0.0");
		this.config = config;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.config.hash(hasher);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return AmalgIO.cache(this.project, this.config.inputsLocal.isEmpty()).resolve("remaps").resolve(AmalgIO.b64(hash));
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder builder, Path resolvedPath, boolean isOutdated) throws IOException {
		var config = this.config;
		Hasher hasher = HASHING.newHasher();
		config.hashMappings(hasher);
		byte[] mappingsHash = hasher.hash().asBytes();

		AmalgRemapper remapper = config.getRemapper();
		if(isOutdated) {
			List<Mappings.Namespaced> maps = new ArrayList<>();
			for(var mapping : config.getMappings()) {
				maps.add(mapping.read());
			}

			config.getRemapper().init(maps);
			this.logger.lifecycle("Remapping " + (config.inputsLocal.size() + config.inputsLocal.size()) + " dependencies");
		}

		this.extracted(resolver, builder, isOutdated, mappingsHash, remapper, config.inputsGlobal, true, isOutdated);
		this.extracted(resolver, builder, isOutdated, mappingsHash, remapper, config.inputsLocal, false, isOutdated);
		this.extracted(resolver, builder, isOutdated, mappingsHash, remapper, config.getClasspath(), false, true);
	}

	private void extracted(TaskInputResolver resolver,
			ZipProcessBuilder builder,
			boolean isOutdated,
			byte[] mappingsHash,
			AmalgRemapper remapper,
			List<Dependency> deps,
			boolean global,
			boolean isClasspath) throws IOException {
		for(Dependency dependency : deps) {
			var dep = new SingleRemapDependency(this.project, remapper, mappingsHash, dependency, global, isClasspath);
			dep.add(resolver, builder, dep.getPath(), isOutdated && dep.isOutdated());
		}
	}
}
