package io.github.f2bb.amalgamation.gradle.plugin.minecraft;

import java.util.Objects;

import io.github.f2bb.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.f2bb.amalgamation.gradle.dependencies.MinecraftDependency;
import io.github.f2bb.amalgamation.gradle.dependencies.RemappingDependency;
import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

public class MinecraftAmalgamationImpl extends BaseAmalgamationImpl implements MinecraftAmalgamation {
	public MinecraftAmalgamationImpl(Project project) {
		super(project);
	}

	@Override
	public Dependency client(String version) {
		return new MinecraftDependency(this.project, version, true);
	}

	@Override
	public Dependency server(String version) {
		return new MinecraftDependency(this.project, version, false);
	}

	@Override
	public Configuration libraries(String version) {
		Configuration configuration = this.project.getConfigurations().create(version + "-libraries");
		configuration.getDependencies().add(new LibrariesDependency(this.project, version));
		return configuration;
	}

	@Override
	public Dependency map(Action<RemappingDependency> mappings) {
		RemappingDependency dependency = new RemappingDependency(this.project);
		mappings.execute(dependency);
		return dependency;
	}
}
