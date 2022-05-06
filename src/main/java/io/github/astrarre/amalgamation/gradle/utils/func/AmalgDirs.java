package io.github.astrarre.amalgamation.gradle.utils.func;

import java.nio.file.Path;
import java.util.function.Function;

import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LazyFunction;
import net.devtech.filepipeline.api.VirtualDirectory;
import org.gradle.api.Project;

public enum AmalgDirs {
	/**
	 * should not be used for artifacts
	 */
	PROJECT(p -> p.getBuildDir().toPath().resolve("amalgamation"), false),
	ROOT_PROJECT(p -> p.getRootProject().getBuildDir().toPath().resolve("amalgamation"), true), // ?
	GLOBAL(p -> p.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation"), true);

	final Function<Project, VirtualDirectory> rootDirectory;

	AmalgDirs(Function<Project, Path> directory, boolean cache) {
		Function<Project, VirtualDirectory> function = p -> AmalgIO.createDir(directory.apply(p)).asDirectory();
		if(cache) {
			function = new LazyFunction<>(function);
		}
		this.rootDirectory = function;
	}

	public VirtualDirectory root(Project project) {
		return this.rootDirectory.apply(project);
	}

	public VirtualDirectory aws(Project project) {
		return AmalgIO.DISK_OUT.outputDir(this.root(project), "accessWideners");
	}

	public VirtualDirectory remaps(Project project) {
		return AmalgIO.DISK_OUT.outputDir(this.root(project), "remaps");
	}

	public VirtualDirectory decomps(Project project) {
		return AmalgIO.DISK_OUT.outputDir(this.root(project), "decompiles");
	}

	public VirtualDirectory unpack(Project project) {
		return AmalgIO.DISK_OUT.outputDir(this.root(project), "unpack");
	}

	public VirtualDirectory downloads(Project project) {
		return AmalgIO.DISK_OUT.outputDir(this.root(project), "downloads");
	}

	public static AmalgDirs of(boolean global) {
		return global ? GLOBAL : ROOT_PROJECT;
	}
}
