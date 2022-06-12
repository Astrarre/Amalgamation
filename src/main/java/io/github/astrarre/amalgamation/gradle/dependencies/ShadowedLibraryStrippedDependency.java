package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import org.gradle.api.Project;

public class ShadowedLibraryStrippedDependency extends CachedDependency {
	public final VirtualPath destinationPath;
	public boolean shouldOutput = true; // todo impl
	public Object toStrip;
	public String unobfPackage = "net/minecraft";
	public String version = "NaN";
	
	public ShadowedLibraryStrippedDependency(Project project, VirtualPath path) {
		super(project);
		this.destinationPath = path;
	}
	
	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.toStrip);
	}
	
	@Override
	protected VirtualPath evaluatePath(byte[] hash) {
		return this.destinationPath;
	}
	
	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		Artifact.File file = new Artifact.File(this.project,
				"net.minecraft",
				"stripped",
				this.version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
		
		VirtualSink sink = AmalgIO.DISK_OUT.subsink(resolvedPath);
		for(Artifact set : this.artifacts(Objects.requireNonNull(this.toStrip, "toStrip was not set"))) {
			VirtualSource source = set.file.openAsSource();
			source.depthStream().filter(VirtualFile.class::isInstance).forEach(virtualPath -> {
				String name = virtualPath.relativePath();
				if(!name.endsWith(".class") || // copy non-classes
				   !name.contains("/") || // copy root dir files
				   name.startsWith("/") && !name.substring(1).contains("/") || // copy root dir files
				   name.contains(this.unobfPackage)) {
					sink.copy(virtualPath, sink.outputFile(name));
				}
			});
		}
		
		return Set.of(file);
	}
}
