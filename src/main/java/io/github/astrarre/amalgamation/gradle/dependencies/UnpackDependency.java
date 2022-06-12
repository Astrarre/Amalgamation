package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.filepipeline.api.VirtualPath;
import org.gradle.api.Project;

public class UnpackDependency extends CachedDependency {
	public final boolean isGlobal;
	public final Set<Artifact> dependency;

	public UnpackDependency(Project project, boolean global, Object dependency) {
		super(project);
		this.isGlobal = global;
		this.dependency = this.artifacts(dependency);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.dependency);
	}

	@Override
	protected VirtualPath evaluatePath(byte[] hash) throws IOException {
		return AmalgDirs.of(this.isGlobal)
		                .unpack(this.project)
		                .getDir(AmalgIO.b64(hash));
	}
	
	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		return null;
	}
}
