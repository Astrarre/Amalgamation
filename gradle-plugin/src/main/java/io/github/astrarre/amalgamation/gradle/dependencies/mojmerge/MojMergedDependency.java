package io.github.astrarre.amalgamation.gradle.dependencies.mojmerge;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.HashedURLDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.MappingTarget;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class MojMergedDependency extends ZipProcessDependency {
	public final String version;
	public final SideAnnotationHandler handler;
	public final Object client;
	public final MappingTarget serverMappings;
	public final MappingTarget clientMappings;

	public MojMergedDependency(Project project,
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
	protected Path evaluatePath(byte[] hash) {
		//String dir = AmalgIO.b64(hash);
		//String name = client.getName() + "-" + server.getName() + "@" + client.getVersion() + "_" + server.getVersion();
		return AmalgIO.cache(this.project, true).resolve(this.version).resolve("merged-" + this.version + ".jar");
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		Artifact.File file = new Artifact.File(
				this.project,
				"net.minecraft",
				"merged",
				this.version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
		if(isOutdated) {
			Mappings.Namespaced server = this.serverMappings.read(), client = this.clientMappings.read();
			process.setEntryProcessor(buffer -> {
				if(buffer.path().endsWith(".class")) {
					ByteBuffer buf = buffer.read();
					ClassReader clientReader = new ClassReader(buf.array(), buf.arrayOffset(), buf.limit());
					ClassWriter writer = new ClassWriter(0);
					MojMerger merger = new MojMerger(Opcodes.ASM9, writer, this.handler, client, server);
					clientReader.accept(merger, 0);
					buffer.writeToOutput(ByteBuffer.wrap(writer.toByteArray()));
				} else {
					buffer.copyToOutput();
				}
				return ProcessResult.HANDLED;
			});
			resolver.apply(this.of(this.client), p -> file);
		} else {
			Object of = this.of(this.client);
			for(TaskTransform transform : resolver.apply(of, p -> null)) {
				transform.setZipFilter(o -> ResourceZipFilter.SKIP);
			}
			process.addProcessed(file);
		}
	}

	public static MappingTarget mojmap(Project project, String version, boolean isClient) {
		Path path = AmalgIO.globalCache(project).resolve(version).resolve((isClient ? "client" : "server") + "_mappings.txt");
		var url = forVers(project, version, isClient);
		HashedURLDependency dependency = new HashedURLDependency(project, url);
		dependency.output = path;
		return new MappingTarget(project, dependency, "source", "target");
	}

	public static LauncherMeta.HashedURL forVers(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return isClient ? vers.getClientMojMap() : vers.getServerMojmap();
	}
}
