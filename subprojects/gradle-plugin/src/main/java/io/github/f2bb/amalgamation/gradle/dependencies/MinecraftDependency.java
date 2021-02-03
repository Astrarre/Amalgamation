package io.github.f2bb.amalgamation.gradle.dependencies;

import java.nio.file.Path;
import java.util.Objects;

import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.jars.StrippedServerJarCachedFile;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MinecraftDependency extends AbstractSelfResolvingDependency {
	private final CachedFile<?> jar;

	public MinecraftDependency(Project project, LauncherMeta meta, String version, boolean isClient) {
		super(project, "net.minecraft", version, isClient ? "minecraft-client" : "minecraft-server");
		LauncherMeta.Version v = Objects.requireNonNull(meta.versions.get(version), "invalid version: " + version);
		Path jar = CachedFile.globalCache(project.getGradle()).resolve(this.getVersion() + "-" + this.getName() + ".jar");
		if (isClient) {
			this.jar = CachedFile.forUrl(v.getClientJar(), jar, project.getLogger());
		} else {
			CachedFile<?> serverJar = CachedFile.forUrl(v.getClientJar(), jar, project.getLogger());
			project.getLogger().lifecycle("getting server without libraries . . .");
			this.jar = new StrippedServerJarCachedFile(jar, serverJar);
		}
	}

	public MinecraftDependency(Project project, String group, String name, String version, CachedFile<?> jar) {
		super(project, group, name, version);
		this.jar = jar;
	}

	@Override
	protected Path resolvePath() {
		return this.jar.getPath();
	}

	@Override
	public Dependency copy() {
		return new MinecraftDependency(this.project, this.group, this.name, this.version, this.jar);
	}
}
