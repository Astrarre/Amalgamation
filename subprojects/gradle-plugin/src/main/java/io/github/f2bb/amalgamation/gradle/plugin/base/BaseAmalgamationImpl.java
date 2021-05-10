package io.github.f2bb.amalgamation.gradle.plugin.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import io.github.f2bb.amalgamation.gradle.dependencies.MergerDependency;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class BaseAmalgamationImpl implements BaseAmalgamation {
	public static final String OPERATING_SYSTEM;
	public static final ExecutorService SERVICE = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> {
		ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		thread.setDaemon(true);
		return thread;
	}, null, true);

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			OPERATING_SYSTEM = "windows";
		} else if (osName.contains("mac")) {
			OPERATING_SYSTEM = "osx";
		} else {
			OPERATING_SYSTEM = "linux";
		}
	}

	protected final Project project;

	public BaseAmalgamationImpl(Project project) {this.project = project;}

	@Override
	public Dependency merge(Action<MergerDependency> configuration) {
		MergerDependency config = new MergerDependency(this.project);
		configuration.execute(config);
		return config;
	}
}
