package io.github.astrarre.amalgamation.gradle.utils.func;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import io.github.astrarre.amalgamation.gradle.utils.LazyFunction;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import org.gradle.api.Project;

public enum AmalgDirs {
	/**
	 * should not be used for artifacts
	 */
	PROJECT(p -> p.getBuildDir().toPath().resolve("amalgamation"), false),
	ROOT_PROJECT(p -> p.getRootProject().getBuildDir().toPath().resolve("amalgamation"), true), // ?
	GLOBAL(p -> p.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation"), true);

	final Function<Project, Path> rootDirectory;

	AmalgDirs(Function<Project, Path> directory, boolean cache) {
		Function<Project, Path> function = p -> {
			Path path = directory.apply(p);
			try {
				Files.createDirectories(path);
			} catch(IOException e) {
				throw Err.rethrow(e);
			}
			return path;
		};
		if(cache) {
			function = new LazyFunction<>(function);
		}
		this.rootDirectory = function;
	}

	public Path root(Project project) {
		return this.rootDirectory.apply(project);
	}

	public Path aws(Project project) {
		return this.root(project);
	}

	public Path remaps(Project project) {
		return this.root(project);
	}

	public Path decomps(Project project) {
		return this.root(project);
	}

	public Path unpack(Project project) {
		return this.root(project);
	}

	public Path downloads(Project project) {
		return this.root(project);
	}

	public static AmalgDirs of(boolean global) {
		return global ? GLOBAL : ROOT_PROJECT;
	}
}
