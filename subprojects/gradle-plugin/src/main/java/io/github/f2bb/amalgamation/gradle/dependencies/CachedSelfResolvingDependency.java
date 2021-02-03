package io.github.f2bb.amalgamation.gradle.dependencies;

import java.net.MalformedURLException;
import java.nio.file.Path;

import io.github.f2bb.amalgamation.gradle.func.CachedResolver;
import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.NotNull;

public class CachedSelfResolvingDependency extends AbstractSelfResolvingDependency {
	private final Project project;
	public final Cache cache;
	public final CachedResolver function;

	/**
	 * @param output $version-$output
	 */
	public CachedSelfResolvingDependency(Project project, String group, String version, String output, Cache cache,
			CachedResolver function) {
		super(project, group, output, version);
		this.project = project;
		this.cache = cache;
		this.function = function;
	}

	@NotNull
	@Override
	public Dependency copy() {
		return new CachedSelfResolvingDependency(this.project, this.group, this.version, this.name, this.cache, this.function);
	}

	@Override
	protected Path resolvePath() {
		try {
			return this.function.resolve(this.cache, this.version + '-' + this.name);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
