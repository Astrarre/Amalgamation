package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.mojmerge.MojMerger;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class MojMergedFile extends ZipProcessCachedFile {
	public final Dependency client;
	public final String version;
	public final CASMerger.Handler handler;
	public final CachedFile serverMappings;

	public MojMergedFile(Path file, Project project, Dependency client, String version, CASMerger.Handler handler, CachedFile serverMappings) {
		super(file, project);
		this.client = client;
		this.version = version;
		this.handler = handler;
		this.serverMappings = serverMappings;
	}

	@Override
	public void hashInputs(Hasher hasher) {
		this.serverMappings.hashInputs(hasher);
		AmalgIO.hash(this.project, hasher, this.client);
	}

	@Override
	public void init(ZipProcessBuilder process, Path outputFile) throws IOException {
		final MemoryMappingTree mappingTree = new MemoryMappingTree(true);
		try(Reader reader = Files.newBufferedReader(this.serverMappings.getOutput())) {
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
		ZipProcessable.add(this.project, process, this.client, p -> outputFile);
	}
}
