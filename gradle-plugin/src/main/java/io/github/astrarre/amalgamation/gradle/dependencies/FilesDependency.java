package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

public class FilesDependency extends AbstractSelfResolvingDependency {
	final Supplier<Iterable<Path>> resolver;
	public FilesDependency(Project project, String group, String name, String version, Supplier<Iterable<Path>> resolver) {
		super(project, group, name, version);
		this.resolver = resolver;
	}

	@Override
	public Dependency copy() {
		return this;
	}

	@Override
	protected Iterable<Path> resolvePaths() throws IOException {
		return resolver.get();
	}
}
