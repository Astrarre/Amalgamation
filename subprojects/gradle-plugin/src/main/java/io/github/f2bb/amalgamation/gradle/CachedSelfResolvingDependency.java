package io.github.f2bb.amalgamation.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

import io.github.f2bb.amalgamation.gradle.func.CachedResolver;
import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency;
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.tasks.TaskDependency;

public class CachedSelfResolvingDependency extends AbstractDependency implements FileCollectionDependency, SelfResolvingDependencyInternal {
	private final Project project;
	public final String group, output, version;
	public final Cache cache;
	public final CachedResolver function;
	private Path resolved;

	/**
	 * @param project
	 * @param output $version-$output
	 */
	public CachedSelfResolvingDependency(Project project, String group, String version, String output, Cache cache,
			CachedResolver function) {
		this.project = project;
		this.group = group;
		this.output = version + '-' + output;
		this.cache = cache;
		this.version = version;
		this.function = function;
	}

	@Override
	public Dependency copy() {
		return new CachedSelfResolvingDependency(this.project, this.group, this.version, this.output, this.cache, this.function);
	}

	private final Path path() {
		if (this.resolved == null) {
			try {
				this.resolved = this.function.resolve(this.cache, this.output);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return this.resolved;
	}

	@Override
	public Set<File> resolve() {
		return this.resolve(true);
	}

	@Override
	public Set<File> resolve(boolean b) {
		return Collections.singleton(this.path().toFile());
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return new AbstractTaskDependency() {
			@Override
			public String toString() {
				return "Dependencies of " + CachedSelfResolvingDependency.this.toString();
			}

			@Override
			public void visitDependencies(TaskDependencyResolveContext context) {
				context.add(CachedSelfResolvingDependency.this);
			}
		};
	}

	@Nullable
	@Override
	public String getGroup() {
		return this.group;
	}

	@Override
	public String getName() {
		return this.output;
	}

	@Nullable
	@Override
	public String getVersion() {
		return this.version;
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		return false;
	}

	@Override
	public FileCollection getFiles() {
		return this.project.files(this.path().toFile());
	}

	@Nullable
	@Override
	public ComponentIdentifier getTargetComponentId() {
		return this::toString;
	}

	@Override
	public String toString() {
		return this.group + ':' + this.version + ':' + this.output;
	}
}
