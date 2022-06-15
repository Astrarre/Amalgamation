package io.github.astrarre.amalgamation.gradle.dependencies.mojmerge;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.HashedURLDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class MojMergedDependency extends CachedDependency {
	public final String version;
	public final SideAnnotationHandler handler;
	public final Object client;
	public final MappingTarget serverMappings;
	public final MappingTarget clientMappings;
	
	public MojMergedDependency(
			Project project,
			String version,
			SideAnnotationHandler handler,
			Object client,
			MappingTarget serverMappings,
			MappingTarget clientMappings) {
		super(project);
		this.version = version;
		this.handler = handler;
		this.client = client;
		this.serverMappings = serverMappings;
		this.clientMappings = clientMappings;
	}
	
	public MojMergedDependency(Project project, String version, SideAnnotationHandler handler, Object client, MappingTarget clientMappings) {
		this(project, version, handler, client, mojmap(project, version, false), clientMappings);
	}
	
	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.client);
		this.serverMappings.hash(hasher);
		this.clientMappings.hash(hasher);
		this.handler.hashInputs(hasher);
	}
	
	@Override
	protected VirtualPath evaluatePath(byte[] hash) {
		//String dir = AmalgIO.b64(hash);
		//String name = client.getName() + "-" + server.getName() + "@" + client.getVersion() + "_" + server.getVersion();
		return AmalgIO.cache(this.project, true).getDir(this.version).getFile("merged-" + this.version + ".jar");
	}
	
	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		Artifact.File file = new Artifact.File(this.project,
				"net.minecraft",
				"merged",
				this.version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
		
		if(isOutdated) {
			// todo remove client mappings since now we have a less shit API, todo consider global cache instead of per-directory cache for speed
			Mappings.Namespaced server = this.serverMappings.read(), client = this.clientMappings.read();
			VirtualSink sink = AmalgIO.DISK_OUT.subsink(resolvedPath);
			for(Artifact artifact : this.artifacts(this.client)) {
				VirtualSource source = artifact.file.openAsSource();
				source.depthStream().filter(VirtualFile.class::isInstance).filter(p -> p.relativePath().endsWith(".class")).forEach(v -> {
					ByteBuffer buf = ((VirtualFile) v).getContents();
					ClassReader clientReader = new ClassReader(buf.array(), buf.arrayOffset(), buf.limit());
					ClassWriter writer = new ClassWriter(0);
					MojMerger merger = new MojMerger(Opcodes.ASM9, writer, this.handler, client, server);
					clientReader.accept(merger, 0);
					sink.write(sink.outputFile(v.relativePath()), ByteBuffer.wrap(writer.toByteArray()));
				});
			}
		}
		
		return Set.of(file);
	}
	
	public static MappingTarget mojmap(Project project, String version, boolean isClient) {
		VirtualFile path = AmalgIO.globalCache(project).getDir(version).getFile((isClient ? "client" : "server") + "_mappings.txt");
		var url = forVers(project, version, isClient);
		HashedURLDependency dependency = new HashedURLDependency(project, url);
		dependency.output = path;
		return new MappingTarget(project, dependency, "target", "source");
	}
	
	public static LauncherMeta.HashedURL forVers(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return isClient ? vers.getClientMojMap() : vers.getServerMojmap();
	}
}
