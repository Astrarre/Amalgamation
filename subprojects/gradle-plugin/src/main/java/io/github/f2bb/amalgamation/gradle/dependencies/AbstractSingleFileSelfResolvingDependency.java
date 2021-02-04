package io.github.f2bb.amalgamation.gradle.dependencies;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal;

public abstract class AbstractSingleFileSelfResolvingDependency extends AbstractSelfResolvingDependency
		implements FileCollectionDependency, SelfResolvingDependencyInternal {

	public AbstractSingleFileSelfResolvingDependency(Project project, String group, String name, String version) {
		super(project, group, name, version);
	}

	@Override
	protected final Collection<Path> resolvePaths() {
		return Collections.singleton(this.resolvePath());
	}

	protected abstract Path resolvePath();
}
