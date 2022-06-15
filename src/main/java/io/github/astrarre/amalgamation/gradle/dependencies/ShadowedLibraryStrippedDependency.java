package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import io.github.astrarre.amalgamation.gradle.utils.zip.ZipIO;
import org.gradle.api.Project;

public class ShadowedLibraryStrippedDependency extends CachedDependency {
	public final Path destinationPath;
	public boolean shouldOutput = true; // todo impl
	public Object toStrip;
	public String unobfPackage = "net/minecraft";
	public String version = "NaN";
	
	public ShadowedLibraryStrippedDependency(Project project, Path path) {
		super(project);
		this.destinationPath = path;
	}
	
	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.toStrip);
	}
	
	@Override
	protected Path evaluatePath(byte[] hash) {
		return this.destinationPath;
	}
	
	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws Exception {
		Artifact.File file = new Artifact.File(this.project,
				"net.minecraft",
				"stripped",
				this.version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
		
		try(FSRef dst = ZipIO.createZip(resolvedPath)) {
			for(Artifact set : this.artifacts(Objects.requireNonNull(this.toStrip, "toStrip was not set"))) {
				FSRef src = ZipIO.readZip(set.file);
				src.directorizingStream(dst).forEach(UCons.of(in -> {
					String name = in.toString();
					if(!name.endsWith(".class") || // copy non-classes
					   !name.contains("/") || // copy root dir files
					   name.startsWith("/") && !name.substring(1).contains("/") || // copy root dir files
					   name.contains(this.unobfPackage)) {
						Files.copy(in, dst.getPath(name));
					}
				}));
			}
		}
		
		return Set.of(file);
	}
}
