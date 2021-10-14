package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.remap.RemapConfig;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

public class DependencyRemapConfig extends RemapConfig {
	final List<Dependency> inputsLocal = new ArrayList<>(), inputsGlobal = new ArrayList<>();

	public DependencyRemapConfig(Project project) {
		super(project);
	}

	public void remap(Object object, boolean useGlobalCache) {
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(this.project.getDependencies().create(object));
	}

	public void remap(Object object, boolean useGlobalCache, Closure<ModuleDependency> config) {
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(this.project.getDependencies().create(object, config));
	}

	@Override
	public void hashMappings(Hasher hasher) throws IOException {
		super.hashMappings(hasher);
		for(Dependency dependency : this.inputsGlobal) {
			this.hashDep(hasher, dependency);
		}

		for(Dependency dependency : this.inputsLocal) {
			this.hashDep(hasher, dependency);
		}
	}
}
