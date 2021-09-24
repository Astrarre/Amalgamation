package io.github.astrarre.amalgamation.gradle.utils.mojmerge;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMergerUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

// todo optimize merger to allow skipMembers, in 1.17+ fields/methods are no longer stripped
public class MojMergerUtil {
	public static void merge(FileSystem client, Path serverMappings, ZipOutputStream merge, CASMerger.Handler handler) throws IOException {
		MemoryMappingTree mappingTree = new MemoryMappingTree(); // todo mapping cache
		try(Reader reader = Files.newBufferedReader(serverMappings)) {
			ProGuardReader.read(reader, mappingTree);
		}

		byte[] reusedBuffer = new byte[1_048_576]; // optimized for mc
		for(Path directory : client.getRootDirectories()) {
			Iterator<Path> files = Files.walk(directory).iterator();
			while(files.hasNext()) {
				Path path = files.next();
				Path relative = directory.relativize(path);
				if(path.toString().endsWith(".class")) {
					byte code[];
					try(InputStream clientStream = Files.newInputStream(path)) {
						// if checkForServerOnly is false, then we can skip debug and code on server
						reusedBuffer = CASMergerUtil.readAll(clientStream, reusedBuffer);
						ClassReader clientReader = new ClassReader(reusedBuffer, 0, reusedBuffer.length);
						ClassWriter writer = new ClassWriter(0);
						MojMerger merger = new MojMerger(Opcodes.ASM9, writer, handler, mappingTree);
						clientReader.accept(merger, 0);
						code = writer.toByteArray();
					}

					merge.putNextEntry(new ZipEntry(relative.toString()));
					merge.write(code);
					merge.closeEntry();
				} else if(!Files.isDirectory(path)) {
					try(InputStream clientStream = Files.newInputStream(path)) {
						merge.putNextEntry(new ZipEntry(relative.toString()));
						reusedBuffer = CASMergerUtil.writeAll(clientStream, merge, reusedBuffer);
						merge.closeEntry();
					}
				}
			}
		}
	}
}
