package io.github.astrarre.amalgamation.gradle.plugin.base;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import io.github.astrarre.amalgamation.gradle.dependencies.MergerDependency;
import io.github.astrarre.amalgamation.gradle.files.SplitClasspathProvider;
import io.github.astrarre.amalgamation.utils.Lazy;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;

public class BaseAmalgamationImpl implements BaseAmalgamation {
	public static final ExecutorService SERVICE = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> {
		ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		thread.setDaemon(true);
		return thread;
	}, null, true);

	public final Project project;
	public final Logger logger;

	public BaseAmalgamationImpl(Project project) {
		this.project = project;
		this.logger = project.getLogger();
	}

	public static Path globalCache(Gradle gradle) {
		return gradle.getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation");
	}

	public static Path projectCache(Project project) {
		return project.getBuildDir().toPath().resolve("amalgamation-caches");
	}

	@Override
	public Dependency merge(Action<MergerDependency> configuration) {
		MergerDependency config = new MergerDependency(this.project);
		configuration.execute(config);
		return config;
	}

	@Override
	public Provider<FileCollection> splitClasspath(Action<ConfigurableFileCollection> config, String... platforms) {
		return this.project.provider(Lazy.of(new SplitClasspathProvider(this.project, config, platforms)));
	}

}
