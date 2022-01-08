package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import org.gradle.api.Project;

public class UnpackDependency extends CachedDependency {
	public final boolean isGlobal;
	public final Set<Artifact> dependency;

	public UnpackDependency(Project project, boolean global, Object dependency) {
		super(project);
		this.isGlobal = global;
		this.dependency = this.artifacts(dependency, true);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.dependency);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return AmalgDirs.of(this.isGlobal)
		                .unpack(this.project)
		                .resolve(AmalgIO.b64(hash));
	}

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		
		return Set.of();
	}

	@Override
	protected Set<Artifact> resolveArtifacts() throws IOException {
		return null;
	}
}
