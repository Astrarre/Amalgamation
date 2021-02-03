package io.github.f2bb.amalgamation.gradle.dependencies;

import java.net.MalformedURLException;
import java.nio.file.Path;

import org.gradle.api.Project;

public abstract class AbstractCachedSelfResolvingDependency extends AbstractSelfResolvingDependency {
	protected final Project project;

	public AbstractCachedSelfResolvingDependency(Project project, String group, String version, String name) {
		super(project, group, name, version);
		this.project = project;
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
