package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.filtr.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.mojmerge.MojMerger;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class MojMergedDependency extends ZipProcessDependency {
	public final CASMerger.Handler handler;
	public final Dependency client;
	public final NamespacedMappingsDependency serverMappings;
	public final NamespacedMappingsDependency clientMappings;

	// todo will require intermediary mappings

	public MojMergedDependency(Project project,
			String version,
			CASMerger.Handler handler,
			Dependency client,
			NamespacedMappingsDependency serverMappings,
			NamespacedMappingsDependency clientMappings) {
		super(project, "io.github.astrarre.amalgamation", "moj-merged", version);
		this.handler = handler;
		this.client = client;
		this.serverMappings = serverMappings;
		this.clientMappings = clientMappings;
	}

	public MojMergedDependency(Project project, String version, CASMerger.Handler handler, Dependency client, NamespacedMappingsDependency clientMappings) {
		this(project, version, handler, client, mojmap(project, version, false), clientMappings);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.client);
		this.hashDep(hasher, this.serverMappings);
		this.handler.hashInputs(hasher);
	}

	@Override
	protected Path evaluatePath(byte[] hash) {
		//String dir = AmalgIO.b64(hash);
		Dependency client = this.client, server = this.serverMappings;
		//String name = client.getName() + "-" + server.getName() + "@" + client.getVersion() + "_" + server.getVersion();
		return AmalgIO.cache(this.project, true).resolve(this.version).resolve("merged-" + this.version + ".jar");
	}

	@Override
	protected void add(ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
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
			ZipProcessable.add(this.project, process, this.of(this.client), p -> tag(resolvedPath));
		} else {
			for(TaskTransform transform : ZipProcessable.add(this.project, process, this.of(this.client), p -> tag(resolvedPath))) {
				transform.setZipFilter(o -> ResourceZipFilter.INVERTED);
			}
			process.addProcessed(resolvedPath);
		}
	}

	public static NamespacedMappingsDependency mojmap(Project project, String version, boolean isClient) {
		Path path = AmalgIO.globalCache(project).resolve(version).resolve((isClient ? "client" : "server") + "_mappings.txt");
		var url = forVers(project, version, isClient);
		HashedURLDependency dependency = new HashedURLDependency(project, url);
		dependency.output = path;
		return new NamespacedMappingsDependency(project, dependency, "source", "target");
	}

	public static LauncherMeta.HashedURL forVers(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return isClient ? vers.getClientMojMap() : vers.getServerMojmap();
	}

}
