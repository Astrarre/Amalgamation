package io.github.f2bb.amalgamation.gradle.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.f2bb.amalgamation.gradle.dependencies.AbstractSelfResolvingDependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MergerConfiguration extends AbstractSelfResolvingDependency {
	private final List<Dependency> unique = new ArrayList<>(), merge = new ArrayList<>();
	public boolean compareInstructions = true;

	public MergerConfiguration(Project project) {
		super(project, "io.github.f2bb", "merged.jar");
	}

	public void includeAll(Iterable<Object> iterator) {
		iterator.forEach(this::include);
	}

	public void addUniqueAll(Iterable<Object> iterator) {
		iterator.forEach(this::addUnique);
	}

	public void include(Object object) {
		this.merge.add(this.project.getDependencies().create(object));
	}

	public void addUnique(Object object) {
		this.unique.add(this.project.getDependencies().create(object));
	}

	@Override
	protected Path resolvePath() {
		return null;
	}

	@Override
	public Dependency copy() {
		return null;
	}
}
