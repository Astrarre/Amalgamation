package io.github.astrarre.amalgamation.gradle.dependencies.cas_merger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import org.gradle.api.Project;

/**
 * client and server merge (combines client and server jars with @Environment annotations)
 */
public class CASMergedDependency extends CachedDependency {
	public final String version;
	public boolean global = true, checkForServerOnly = false;
	public SideAnnotationHandler handler = SideAnnotationHandler.FABRIC;
	public Object client, server;
	
	public CASMergedDependency(Project project, String version) {
		super(project);
		this.version = version;
	}
	
	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		Objects.requireNonNull(this.client, "client dependency was not set!");
		Objects.requireNonNull(this.server, "server dependency was not set!");
		Objects.requireNonNull(this.handler, "annotation handler was not set!");
		this.hashDep(hasher, this.client);
		this.hashDep(hasher, this.server);
		this.handler.hashInputs(hasher);
	}
	
	@Override
	protected VirtualPath evaluatePath(byte[] hash) {
		String dir = AmalgIO.b64(hash);
		return AmalgIO.cache(this.project, this.global).getDir(this.version).getFile(this.version + "-cas-merged-" + dir + ".jar");
	}
	
	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		Objects.requireNonNull(this.client, "client dependency was not set!");
		Objects.requireNonNull(this.server, "server dependency was not set!");
		Objects.requireNonNull(this.handler, "annotation handler was not set!");
		Artifact.File returnArtifact = new Artifact.File(this.project,
				"net.minecraft",
				"merged",
				this.version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
		
		if(isOutdated) {
			CASMergerUtil merger = new CASMergerUtil(this.handler, this.checkForServerOnly);
			for(Artifact artifact : this.artifacts(this.of(this.server))) {
				VirtualSource source = artifact.file.openOrThrow();
				CASMergerUtil.ServerCollector collector = merger.serverScan();
				source
						.depthStream()
						.filter(v -> v.relativePath().endsWith(".class"))
						.filter(VirtualFile.class::isInstance)
						.map(VirtualFile.class::cast)
						.forEach(v -> collector.collect(v.relativePath(), v.getContents()));
			}
			
			VirtualSink sink = AmalgIO.DISK_OUT.subsink(resolvedPath);
			for(Artifact artifact : this.artifacts(this.of(this.client))) {
				CASMergerUtil.ClassAnnotater collector = merger.clientApplier();
				VirtualSource source = artifact.file.openOrThrow();
				source.depthStream().filter(VirtualFile.class::isInstance).forEach(path -> {
					String path1 = path.relativePath();
					if(path1.endsWith(".class")) {
						ByteBuffer apply = collector.apply(path1, path.asFile().getContents());
						sink.write(sink.outputFile(path1), apply);
					} else {
						sink.copy(path, sink.outputFile(path1));
					}
				});
			}
		}
		return Set.of(returnArtifact);
	}
}
