package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.LibraryStrippedFile;
import io.github.astrarre.amalgamation.gradle.files.MinecraftFile;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.FileUtil;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.LazySet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MinecraftDependency extends AbstractSelfResolvingDependency {
	private final CachedFile<?> jar;
	private final boolean doSplit, doStrip;

	/**
	 * @param doSplit if the jar should be split into a classes jar and resources jar, improves dev init time
	 * @param doStrip if the jar should be stripped of libraries, eg. at the time of this writing, the minecraft server shades all it's libraries, and they need to be stripped to avoid duplication
	 */
	public MinecraftDependency(Project project, String version, boolean isClient, boolean doSplit, boolean doStrip) {
		super(project, "net.minecraft", version, isClient ? "minecraft-client" : "minecraft-server");
		this.doSplit = doSplit;
		this.doStrip = doSplit;
		if (doSplit) {
			LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project);
			LauncherMeta.Version vers = meta.getVersion(version);
			LauncherMeta.HashedURL url;
			String area;
			if (isClient) {
				url = vers.getClientJar();
				if(doStrip) {
					area = "client";
				} else {
					area = "client-unstripped";
				}
			} else {
				url = vers.getServerJar();
				if(doStrip) {
					area = "server";
				} else {
					area = "server-unstripped";
				}
			}
			Path jar = FileUtil.globalCache(project.getGradle()).resolve(version).resolve(area);
			this.jar = new MinecraftFile(jar, url, project.getLogger(), false, isClient, doStrip);
		} else {
			LauncherMeta.Version v = Objects.requireNonNull(MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version),
					"invalid version: " + version);
			Path globalCache = FileUtil.globalCache(project.getGradle());
			Path jar = globalCache.resolve(this.getVersion() + "-" + this.getName() + ".jar");
			Path unstripped = globalCache.resolve(this.getVersion() + "-" + this.getName() + "-unstripped.jar");
			LauncherMeta.HashedURL url;
			if(isClient) {
				url = v.getClientJar();
			} else {
				url = v.getServerJar();
			}

			CachedFile<?> file = CachedFile.forUrl(url, unstripped, project.getLogger(), false);
			if(doStrip) {
				this.jar = new LibraryStrippedFile(jar, file);
			} else {
				this.jar = file;
			}
		}
	}

	public MinecraftDependency(Project project, String group, String name, String version, CachedFile<?> jar, boolean doSplit, boolean doStrip) {
		super(project, group, name, version);
		this.doSplit = doSplit;
		this.doStrip = doStrip;
		this.jar = jar;
	}

	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			this.resolved = new LazySet(CompletableFuture.supplyAsync(() -> {
				if(doSplit) {
					Set<File> files = new HashSet<>(2);
					files.add(this.jar.getPath().resolve("classes.jar").toFile());
					files.add(this.jar.getPath().resolve("resources.jar").toFile());
					return files;
				} else {
					return Collections.singleton(this.jar.getPath().toFile());
				}
			}, Constants.SERVICE));
		}
		return this.resolved;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dependency copy() {
		return new MinecraftDependency(this.project, this.group, this.name, this.version, this.jar, this.doSplit, this.doStrip);
	}
}
