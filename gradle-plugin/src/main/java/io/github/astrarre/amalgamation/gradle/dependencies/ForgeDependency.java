package io.github.astrarre.amalgamation.gradle.dependencies;

public class ForgeDependency {} /*extends AbstractSingleFileSelfResolvingDependency {
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
		return this.file.getOutput();
	}

	@Override
	public Dependency copy() {
		return new ForgeDependency(this.project, this.group, this.name, this.version, this.minecraft, this.libraries, this.forgeVersion);
	}
}*/
