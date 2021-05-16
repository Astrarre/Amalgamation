package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.github.astrarre.amalgamation.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.jars.StrippedServerJarCachedFile;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.utils.CachedFile;
import io.github.astrarre.amalgamation.gradle.util.LazySet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MinecraftDependency extends AbstractSingleFileSelfResolvingDependency {
	private final CachedFile<?> jar;

	public MinecraftDependency(Project project, String version, boolean isClient) {
		super(project, "net.minecraft", version, isClient ? "minecraft-client" : "minecraft-server");
		LauncherMeta.Version v = Objects.requireNonNull(MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version),
				"invalid version: " + version);
		Path jar = BaseAmalgamationImpl.globalCache(project.getGradle()).resolve(this.getVersion() + "-" + this.getName() + ".jar");
		if (isClient) {
			this.jar = CachedFile.forUrl(v.getClientJar(), jar, project.getLogger());
		} else {
			CachedFile<?> serverJar = CachedFile.forUrl(v.getServerJar(), jar, project.getLogger());
			project.getLogger().lifecycle("getting server without libraries . . .");
			this.jar = new StrippedServerJarCachedFile(jar, serverJar);
		}
	}

	public MinecraftDependency(Project project, String group, String name, String version, CachedFile<?> jar) {
		super(project, group, name, version);
		this.jar = jar;
	}

	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			this.resolved = new LazySet(CompletableFuture.supplyAsync(() -> Collections.singleton(this.jar.getOutdatedPath().toFile()),
					BaseAmalgamationImpl.SERVICE));
		}
		return this.resolved;
	}

	@Override
	protected Path resolvePath() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dependency copy() {
		return new MinecraftDependency(this.project, this.group, this.name, this.version, this.jar);
	}
}
