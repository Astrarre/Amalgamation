package io.github.f2bb.amalgamation.gradle.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

public class LibrariesDependency extends AbstractSelfResolvingDependency {
	public LibrariesDependency(Project project, String version) {
		super(project, "net.minecraft", "minecraft-libraries", version);
		
	}

	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
			Configuration configuration = this.project.getConfigurations().detachedConfiguration();
			for (String library : Objects.requireNonNull(meta.getVersion(this.version), "Invalid version: " + this.version).getLibraries()) {
				configuration.getDependencies().add(this.project.getDependencies().create(library));
			}
			this.resolved = configuration.resolve();
		}
		return this.resolved;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dependency copy() {
		return new LibrariesDependency(this.project, this.version);
	}
}
