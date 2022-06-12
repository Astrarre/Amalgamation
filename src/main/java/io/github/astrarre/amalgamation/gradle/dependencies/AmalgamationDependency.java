package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.filepipeline.impl.util.FPInternal;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.logging.Logger;

public abstract class AmalgamationDependency extends AbstractSet<Object> {
	public final Project project;
	public final Logger logger;
	private Set<Artifact> artifacts;

	public AmalgamationDependency(Project project) {
		this.project = project;
		this.logger = project.getLogger();
	}

	public Set<Artifact> getArtifacts() {
		if(this.artifacts == null) {
			try {
				this.artifacts = this.resolveArtifacts();
			} catch(IOException e) {
				throw FPInternal.rethrow(e);
			}
		}
		return this.artifacts;
	}

	@Override
	public Iterator<Object> iterator() {
		return this.getArtifacts()
				       .stream()
				       .filter(a -> a.type != Artifact.Type.SOURCES)
				       .peek(Artifact::makeGradleFriendly)
				       .map(Artifact::toDependencyNotation)
				       .iterator();
	}

	@Override
	public int size() {
		return Iterators.size(iterator());
	}

	protected abstract Set<Artifact> resolveArtifacts() throws IOException;

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

	protected Set<Artifact> artifacts(Object notation) {
		return artifacts(notation, false);
	}

	protected Set<Artifact> artifacts(Object notation, boolean isOutput) {
		if(notation instanceof AmalgamationDependency a) {
			return a.getArtifacts();
		}
		Dependency dep;
		if(notation instanceof Dependency d) {
			dep = d;
		} else {
			dep = this.project.getDependencies().create(notation);
		}
		Set<Artifact> artifacts = new HashSet<>();
		Map<ComponentIdentifier, byte[]> dependencyIds = new HashMap<>();
		Set<ResolvedDependency> dependencies = new HashSet<>();
		List<Dependency> unresolved = new ArrayList<>();
		AmalgIO.resolveDeps(this.project, Set.of(dep), dependencies, unresolved);
		for(ResolvedDependency resolved : dependencies) {
			this.artifacts(resolved, artifacts, dependencyIds);
		}

		for(Dependency dependency : unresolved) {
			for(File file : AmalgIO.resolve(project, List.of(dependency))) {
				Hasher hasher = AmalgIO.SHA256.newHasher();
				AmalgIO.hash(hasher, file);
				Artifact.File fact = new Artifact.File(
						this.project,
						dependency.getGroup(),
						dependency.getName(),
						dependency.getVersion(),
						AmalgIO.resolve(file.toPath()),
						hasher.hash().asBytes(),
						Artifact.Type.MIXED);
				artifacts.add(fact);
			}
		}

		AmalgIO.getSources(project, dependencyIds.keySet())
				.map(result -> new Artifact.Maven(project, result.getId(), result.getFile(), "sources", dependencyIds.get(result.getId().getComponentIdentifier())))
				.forEach(artifacts::add);

		for(Artifact artifact : artifacts) {
			if(artifact instanceof Artifact.Maven m) {
				m.isOutput = isOutput;
			}
		}
		return artifacts;
	}

	protected void artifacts(ResolvedDependency dependency, Set<Artifact> artifacts, Map<ComponentIdentifier, byte[]> ids) {
		Set<ResolvedArtifact> arts = dependency.getModuleArtifacts();
		Hasher hasher = AmalgIO.SHA256.newHasher();
		for(ResolvedArtifact art : arts) {
			AmalgIO.hash(hasher, art.getFile());
		}
		byte[] hash = hasher.hash().asBytes();
		for(ResolvedArtifact artifact : arts) {
			ids.put(artifact.getId().getComponentIdentifier(), hash);
			Artifact.Maven fact = new Artifact.Maven(this.project, artifact.getId(), artifact.getFile(), artifact.getClassifier(), hash);
			artifacts.add(fact);
		}
	}

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return getClass().getName() + "@" + Integer.toHexString(hashCode());
	}
}
