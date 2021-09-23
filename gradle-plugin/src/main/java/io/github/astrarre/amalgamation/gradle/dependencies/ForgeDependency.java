package io.github.astrarre.amalgamation.gradle.dependencies;

import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.ForgeFile;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class ForgeDependency extends AbstractSingleFileSelfResolvingDependency {
	public final MinecraftDependency minecraft;
	public final LibrariesDependency libraries;
	public final CachedFile<?> file;
	public final String forgeVersion;
	public ForgeDependency(Project project,
			String group,
			String name,
			String version,
			MinecraftDependency minecraft,
			LibrariesDependency libraries,
			String forgeVersion) {
		super(project, group, name, version);
		this.minecraft = minecraft;
		this.libraries = libraries;
		this.forgeVersion = forgeVersion;
		this.file = new ForgeFile(minecraft.isClient, minecraft, libraries, forgeVersion);
	}

	@Override
	protected Path resolvePath() {
		return this.file.getPath();
	}

	@Override
	public Dependency copy() {
		return new ForgeDependency(this.project, this.group, this.name, this.version, this.minecraft, this.libraries, this.forgeVersion);
	}
}
