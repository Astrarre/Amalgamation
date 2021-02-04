package io.github.f2bb.amalgamation.gradle.plugin.minecraft;

import java.util.Objects;

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
		return new MinecraftDependency(project, version, true);
	}

	@Override
	public Dependency server(String version) {
		return new MinecraftDependency(project, version, false);
	}

	@Override
	public Configuration libraries(String version) {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		Configuration configuration = this.project.getConfigurations().create(version + "-libraries");
		for (String library : Objects.requireNonNull(meta.versions.get(version), "Invalid version: " + version).getLibraries()) {
			configuration.getDependencies().add(this.project.getDependencies().create(library));
		}
		return configuration;
	}

	@Override
	public Dependency map(Action<RemappingDependency> mappings) {
		RemappingDependency dependency = new RemappingDependency(this.project);
		mappings.execute(dependency);
		return dependency;
	}
}
