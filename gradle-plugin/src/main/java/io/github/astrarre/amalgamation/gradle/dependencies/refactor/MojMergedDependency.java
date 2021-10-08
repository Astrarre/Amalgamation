package io.github.astrarre.amalgamation.gradle.dependencies.refactor;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.refactor.filtr.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.mojmerge.MojMerger;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class MojMergedDependency extends ZipProcessDependency {
	public final CASMerger.Handler handler;
	public final Dependency client;
	public final Dependency serverMappings;

	// todo will require intermediary mappings

	public MojMergedDependency(Project project, String version, CASMerger.Handler handler, Dependency client, Dependency serverMappings) {
		super(project, "io.github.astrarre.amalgamation", "moj-merged", version);
		this.handler = handler;
		this.client = client;
		this.serverMappings = serverMappings;
	}

	public MojMergedDependency(Project project, String version, CASMerger.Handler handler, Dependency client) {
		this(project, version, handler, client, mojmap(project, version, false));
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
			final MemoryMappingTree mappingTree = new MemoryMappingTree(true);
			try(Reader reader = URLDependency.read(this.project, this.of(this.serverMappings))) {
				ProGuardReader.read(reader, mappingTree);
			}
			process.setEntryProcessor(buffer -> {
				if(buffer.path().endsWith(".class")) {
					ByteBuffer buf = buffer.read();
					ClassReader clientReader = new ClassReader(buf.array(), buf.arrayOffset(), buf.limit());
					ClassWriter writer = new ClassWriter(0);
					MojMerger merger = new MojMerger(Opcodes.ASM9, writer, this.handler, mappingTree);
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

	public static HashedURLDependency mojmap(Project project, String version, boolean isClient) {
		Path path = AmalgIO.globalCache(project).resolve(version).resolve((isClient ? "client" : "server") + "_mappings.txt");
		var url = forVers(project, version, isClient);
		HashedURLDependency dependency = new HashedURLDependency(project, url);
		dependency.output = path;
		return dependency;
	}

	public static LauncherMeta.HashedURL forVers(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return isClient ? vers.getClientMojMap() : vers.getServerMojmap();
	}

}
