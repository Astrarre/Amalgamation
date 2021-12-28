package io.github.astrarre.amalgamation.gradle.plugin.base.mvn;

import java.util.HashSet;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.plugin.base.Dependent;
import io.github.astrarre.amalgamation.gradle.utils.Lazy;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.jetbrains.annotations.Nullable;

public class ConfigurationExcluder implements MvnMetaReader.DependencyVisitor {
	final Lazy<Set<Dependent>> toRemove;

	public ConfigurationExcluder(Configuration configuration) {
		this.toRemove = Lazy.of(() -> {
			if(!configuration.isCanBeResolved()) {
				configuration.setCanBeResolved(true); // maybe copy idk
			}
			ResolvedConfiguration config = configuration.getResolvedConfiguration();
			Set<ResolvedDependency> dependencies = new HashSet<>();
			config.getFirstLevelModuleDependencies().forEach(r -> this.findAll(r, dependencies));

			Set<Dependent> toRemove = new HashSet<>();
			for(ResolvedDependency dependency : dependencies) {
				toRemove.add(new Dependent(dependency.getModuleGroup(), dependency.getModuleName(), dependency.getModuleVersion()));
			}
			return toRemove;
		});
	}

	@Override
	public boolean apply(@Nullable String group, String name, @Nullable String version, MvnMetaReader.Mutator mutator) {
		return this.toRemove.get().contains(new Dependent(group, name, version));
	}

	void findAll(ResolvedDependency dependency, Set<ResolvedDependency> dependencies) {
		if(dependencies.add(dependency)) {
			for(ResolvedDependency child : dependency.getChildren()) {
				this.findAll(child, dependencies);
			}
		}
	}
}
