package io.github.astrarre.amalgamation.gradle.plugin.base;

import java.io.File;
import java.util.function.Supplier;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.dependencies.DeJiJDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.MergerDependency;
import io.github.astrarre.amalgamation.gradle.files.SplitClasspathProvider;
import io.github.astrarre.amalgamation.gradle.utils.AmalgamationIO;
import io.github.astrarre.amalgamation.gradle.utils.Lazy;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;

public class BaseAmalgamationImpl implements BaseAmalgamation {
	public final Project project;
	public final Logger logger;

	public BaseAmalgamationImpl(Project project) {
		this.project = project;
		this.logger = project.getLogger();
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

	@Override
	public <T> Provider<T> provideLazy(Supplier<T> action) {
		return this.project.provider(Lazy.of(action));
	}

	@Override
	public Provider<Iterable<File>> resolve(Iterable<Object> dependency) {
		return this.provideLazy(() -> {
			DependencyHandler handler = this.project.getDependencies();
			return AmalgamationIO.resolve(this.project, Iterables.transform(dependency, handler::create));
		});
	}

	@Override
	public Dependency deJiJ(String name, Action<DeJiJDependency> configuration) {
		DeJiJDependency dependency = new DeJiJDependency(this.project, name);
		configuration.execute(dependency);
		return dependency;
	}

}
