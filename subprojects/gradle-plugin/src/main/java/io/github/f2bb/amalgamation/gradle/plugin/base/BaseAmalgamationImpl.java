package io.github.f2bb.amalgamation.gradle.plugin.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.f2bb.amalgamation.gradle.dependencies.MergerDependency;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class BaseAmalgamationImpl implements BaseAmalgamation {
	public static final ExecutorService SERVICE = Executors.newWorkStealingPool();
	protected final Project project;

	public BaseAmalgamationImpl(Project project) {this.project = project;}

	@Override
	public Dependency merge(Action<MergerDependency> configuration) {
		MergerDependency config = new MergerDependency(this.project);
		configuration.execute(config);
		return config;
	}
}
