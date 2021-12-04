package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class AmalgamationDependency extends AbstractSet<Object> {
	public final Project project;
	public final Logger logger;
	private List<Artifact> artifacts;

	public AmalgamationDependency(Project project) {
		this.project = project;
		this.logger = project.getLogger();
	}

	public List<Artifact> getArtifacts() {
		if(this.artifacts == null) {
			try {
				this.artifacts = this.resolveArtifacts();
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
		return this.artifacts;
	}

	@Override
	public Iterator<Object> iterator() {
		return Iterators.transform(this.artifacts.iterator(), Artifact::toDependencyNotation);
	}

	@Override
	public int size() {
		return this.artifacts.size();
	}

	protected abstract List<Artifact> resolveArtifacts() throws IOException;

	public Object of(Object notation) {
		if(notation instanceof AmalgamationDependency a) {
			return a;
		}
		return this.project.getDependencies().create(notation);
	}

	public Object of(Object notation, Closure<ModuleDependency> config) {
		if(notation instanceof AmalgamationDependency a) {
			return a;
		}
		return this.project.getDependencies().create(notation, config);
	}

	protected List<Artifact> artifacts(Object notation, boolean resolve) {
		if(notation instanceof ZipProcessDependency d && resolve) {
			try {
				return Streams.stream(d.process().getOutputs())
						       .map(Artifact.class::cast)
						       .toList();
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		} else if(notation instanceof AmalgamationDependency a) {
			return a.getArtifacts();
		}
		Dependency dep;
		if(notation instanceof Dependency d) {
			dep = d;
		} else {
			dep = this.project.getDependencies().create(notation);
		}
		List<Artifact> artifacts = new ArrayList<>();
		List<ComponentIdentifier> dependencyIds = new ArrayList<>();
		for(ResolvedDependency resolved : AmalgIO.resolveDeps(this.project, List.of(dep))) {
			this.artifacts(resolved, artifacts, dependencyIds);
		}
		AmalgIO.getSources(project, dependencyIds)
				.map(result -> new Artifact.File(project, result.getId(), result.getFile(), "sources"))
				.forEach(artifacts::add);
		return artifacts;
	}

	protected void artifacts(ResolvedDependency dependency, List<Artifact> artifacts, List<ComponentIdentifier> ids) {
		for(ResolvedArtifact artifact : dependency.getArtifacts(dependency)) {
			ids.add(artifact.getId().getComponentIdentifier());
			Artifact.File fact = new Artifact.File(this.project, artifact.getId(), artifact.getFile(), artifact.getClassifier());
			artifacts.add(fact);
		}
	}

	protected static Artifact derive(OutputTag tag, Path path, @Nullable byte[] hash) {
		if(tag instanceof Artifact a) {
			return a.derive(path, hash);
		} else {
			throw new IllegalArgumentException("Unrecognized tag type: " + tag.getClass());
		}
	}
}
