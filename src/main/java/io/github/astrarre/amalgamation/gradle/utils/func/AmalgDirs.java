package io.github.astrarre.amalgamation.gradle.utils.func;

import java.nio.file.Path;
import java.util.function.Function;

import io.github.astrarre.amalgamation.gradle.utils.LazyFunction;
import org.gradle.api.Project;

public enum AmalgDirs {
	/**
	 * should not be used for artifacts
	 */
	PROJECT(p -> p.getBuildDir().toPath().resolve("amalgamation")),
	ROOT_PROJECT(p -> PROJECT.root(p.getRootProject())),
	GLOBAL(new LazyFunction<>(p -> p.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation")));

	final Function<Project, Path> rootDirectory;

	AmalgDirs(Function<Project, Path> directory) {
		this.rootDirectory = directory;
	}

	public Path root(Project project) {
		return this.rootDirectory.apply(project);
	}

	public Path aws(Project project) {
		return this.root(project).resolve("accessWideners");
	}

	public Path remaps(Project project) {
		return this.root(project).resolve("remaps");
	}

	public Path decomps(Project project) {
		return this.root(project).resolve("decompiles");
	}

	public Path unpack(Project project) {
		return this.root(project).resolve("unpack");
	}

	public static AmalgDirs of(boolean global) {
		return global ? GLOBAL : ROOT_PROJECT;
	}
}
