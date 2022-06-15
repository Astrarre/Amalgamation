package io.github.astrarre.amalgamation.gradle.dependencies.cas_merger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import java.nio.file.Path;
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import io.github.astrarre.amalgamation.gradle.utils.zip.ZipIO;
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
	protected Path evaluatePath(byte[] hash) {
		String dir = AmalgIO.b64(hash);
		return AmalgIO.cache(this.project, this.global).resolve(this.version).resolve(this.version + "-cas-merged-" + dir + ".jar");
	}
	
	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws Exception {
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
				CASMergerUtil.ServerCollector collector = merger.serverScan();
				FSRef ref = ZipIO.readZip(artifact.file);
				ref.walk().filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".class")).forEach(path -> {
					collector.collect(path.toString(), AmalgIO.readAll(path));
				});
			}
			
			try(FSRef output = ZipIO.createZip(returnArtifact.file)) {
				for(Artifact artifact : this.artifacts(this.of(this.client))) {
					CASMergerUtil.ClassAnnotater collector = merger.clientApplier();
					FSRef ref = ZipIO.readZip(artifact.file);
					ref.directorizingStream(output).forEach(UCons.of(path -> {
						String name = path.toString();
						if(name.endsWith(".class")) {
							ByteBuffer apply = collector.apply(name, AmalgIO.readAll(path));
							AmalgIO.write(path, apply);
						} else {
							Files.copy(path, output.getPath(name));
						}
					}));
				}
			}
		}
		return Set.of(returnArtifact);
	}
}
