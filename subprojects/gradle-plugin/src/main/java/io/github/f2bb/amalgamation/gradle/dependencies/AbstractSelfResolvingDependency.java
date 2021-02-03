package io.github.f2bb.amalgamation.gradle.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

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
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSelfResolvingDependency extends AbstractDependency
		implements FileCollectionDependency, SelfResolvingDependencyInternal {
	protected final Project project;
	protected final String group, name, version;
	protected Path resolved;

	public AbstractSelfResolvingDependency(Project project, String group, String name, String version) {
		this.project = project;
		this.group = group;
		this.name = name;
		this.version = version;
	}

	@Override
	public boolean contentEquals(@NotNull Dependency dependency) {
		return this.equals(dependency);
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return new AbstractTaskDependency() {
			@Override
			public String toString() {
				return "Dependencies of " + AbstractSelfResolvingDependency.this.toString();
			}

			@Override
			public void visitDependencies(TaskDependencyResolveContext context) {
				context.add(AbstractSelfResolvingDependency.this);
			}
		};
	}

	@Override
	public Set<File> resolve() {
		return this.resolve(true);
	}

	private Path path() {
		if (this.resolved == null) {
			this.resolved = this.resolvePath();
		}
		return this.resolved;
	}

	protected abstract Path resolvePath();

	@Override
	public Set<File> resolve(boolean b) {
		return Collections.singleton(this.path().toFile());
	}

	@Nullable
	@Override
	public String getGroup() {
		return this.group;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Nullable
	@Override
	public String getVersion() {
		return this.version;
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

	@NotNull
	@Override
	public String toString() {
		return this.group + ':' + this.name + ':' + this.version;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof AbstractSelfResolvingDependency)) {
			return false;
		}

		AbstractSelfResolvingDependency that = (AbstractSelfResolvingDependency) object;

		if (!Objects.equals(this.project, that.project)) {
			return false;
		}
		if (!Objects.equals(this.group, that.group)) {
			return false;
		}
		if (!Objects.equals(this.name, that.name)) {
			return false;
		}
		if (!Objects.equals(this.version, that.version)) {
			return false;
		}
		return Objects.equals(this.resolved, that.resolved);
	}

	@Override
	public int hashCode() {
		int result = this.project != null ? this.project.hashCode() : 0;
		result = 31 * result + (this.group != null ? this.group.hashCode() : 0);
		result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		result = 31 * result + (this.resolved != null ? this.resolved.hashCode() : 0);
		return result;
	}
}
