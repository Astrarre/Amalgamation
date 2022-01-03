package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import org.gradle.api.artifacts.ModuleDependency;

public class RemapDependencyConfig extends RemapConfig {
	final List<SingleRemapDependency> inputsLocal = new ArrayList<>(), inputsGlobal = new ArrayList<>();
	final RemapDependency dependency;

	public RemapDependencyConfig(RemapDependency dependency) {
		super(dependency.project);
		this.dependency = dependency;
	}

	public Object remap(Object object, boolean useGlobalCache) {
		SingleRemapDependency dep = this.dependency.createDep(this.of(object), useGlobalCache ? AmalgDirs.GLOBAL : AmalgDirs.ROOT_PROJECT, false);
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(dep);
		return dep;
	}

	public Object remap(Object object, Closure<ModuleDependency> config, boolean useGlobalCache) {
		Object dependency = this.of(object, config);
		SingleRemapDependency dep = this.dependency.createDep(dependency, useGlobalCache ? AmalgDirs.GLOBAL : AmalgDirs.ROOT_PROJECT, false);
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(dep);
		return dep;
	}

	/**
	 * add an input to be remapped
	 * @param object the remapped version of the dependency, evaluation will trigger the whole remap to start
	 */
	public Object inputLocal(Object object) {
		return this.remap(object, false);
	}

	public Object inputLocal(Object object, Closure<ModuleDependency> config) {
		return this.remap(object, config, false);
	}

	public Object inputGlobal(Object object) {
		return this.remap(object, true);
	}

	public Object inputGlobal(Object object, Closure<ModuleDependency> config) {
		return this.remap(object, config, true);
	}

	@Override
	public void hash(Hasher hasher) throws IOException {
		super.hash(hasher);
		for(var dependency : this.inputsGlobal) {
			AmalgIO.hashDep(hasher, this.project, dependency.dependency);
		}
		for(var dependency : this.inputsLocal) {
			AmalgIO.hashDep(hasher, this.project, dependency.dependency);
		}
	}
}
