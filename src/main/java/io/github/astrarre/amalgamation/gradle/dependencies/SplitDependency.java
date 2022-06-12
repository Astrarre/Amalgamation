package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.filepipeline.api.VirtualDirectory;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class SplitDependency extends CachedDependency {
	public final Dependency dependency;
	public VirtualPath outputDir;
	
	public SplitDependency(Project project, Dependency dependency) {
		super(project);
		this.dependency = dependency;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.dependency);
	}

	@Override
	protected VirtualPath evaluatePath(byte[] hash) throws MalformedURLException {
		return this.outputDir;
	}

	Artifact.File artifact(VirtualPath dest, Artifact.Type type) {
		return new Artifact.File(
				this.project,
				this.dependency.getGroup(),
				this.dependency.getName() + type.classifier,
				this.dependency.getVersion(),
				dest,
				this.getCurrentHash(),
				type
		);
	}
	
	private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);
	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		AmalgIO.DISK_OUT.createIfAbsent(resolvedPath);
		VirtualDirectory directory = resolvedPath.asDir();
		AmalgIO.DISK_OUT.write(directory.getFile("resources.jar.rss_marker"), EMPTY);
		
		Artifact cls = this.artifact(directory.getFile("classes.jar"), Artifact.Type.MIXED);
		Artifact rss = this.artifact(directory.getFile("resources.jar"), Artifact.Type.RESOURCES);
		Artifact src = this.artifact(directory.getFile("sources.jar"), Artifact.Type.SOURCES);
		
		if(isOutdated) {
			VirtualSink classes = AmalgIO.DISK_OUT.subsink(cls.file);
			VirtualSink resources = AmalgIO.DISK_OUT.subsink(rss.file);
			VirtualSink sources = AmalgIO.DISK_OUT.subsink(src.file);
			for(Artifact artifact : this.artifacts(this.dependency)) {
				VirtualSource source = artifact.file.openOrThrow();
				source.depthStream().filter(VirtualFile.class::isInstance).forEach(virtualPath -> {
					String path = virtualPath.relativePath();
					if(path.endsWith(".class") || path.contains("META-INF")) {
						classes.copy(virtualPath, classes.outputFile(path));
					} else if(path.endsWith(".java")) {
						sources.copy(virtualPath, sources.outputFile(path));
					} else {
						resources.copy(virtualPath, resources.outputFile(path));
					}
				});
			}
		}
		return Set.of(cls, rss, src);
	}
	
}
