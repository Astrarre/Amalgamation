package io.github.astrarre.amalgamation.gradle.utils.casmerge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * Client & Server Merger Utility
 */
public class CASMergerUtil {
	public static void merge(CASMerger.Handler handler,
			FileSystem client,
			FileSystem server,
			ZipOutputStream merge,
			int classReaderSettings,
			boolean checkForServerOnly) throws IOException {
		byte[] reusedBuffer = new byte[1_048_576]; // optimized for mc
		for(Path directory : client.getRootDirectories()) {
			Iterator<Path> files = Files.walk(directory).iterator();
			while(files.hasNext()) {
				Path clientFile = files.next();
				Path relative = directory.relativize(clientFile);
				if(clientFile.toString().endsWith(".class")) { // todo why this brok
					Path serverFile = server.getPath(relative.toString());
					if(Files.exists(serverFile)) {
						byte code[];
						try(InputStream clientStream = Files.newInputStream(clientFile); InputStream serverStream = Files.newInputStream(serverFile)) {
							// if checkForServerOnly is false, then we can skip debug and code on server
							reusedBuffer = readAll(clientStream, reusedBuffer);
							ClassReader serverReader = new ClassReader(reusedBuffer, 0, reusedBuffer.length); // length is not used
							ClassNode serverNode = new ClassNode();
							serverReader.accept(
									serverNode,
									checkForServerOnly ? classReaderSettings :
									(ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG));
							reusedBuffer = readAll(serverStream, reusedBuffer);
							ClassReader clientReader = new ClassReader(reusedBuffer, 0, reusedBuffer.length);
							ClassWriter writer = new ClassWriter(0);
							CASMerger merger = new CASMerger(writer, Opcodes.ASM9, serverNode, handler, checkForServerOnly);
							clientReader.accept(merger, classReaderSettings);
							code = writer.toByteArray();
						}

						merge.putNextEntry(new ZipEntry(relative.toString()));
						merge.write(code);
						merge.closeEntry();
					} else {
						reusedBuffer = annotate(handler, merge, reusedBuffer, clientFile, relative, true);
					}
				} else if(!Files.isDirectory(clientFile)) {
					try(InputStream clientStream = Files.newInputStream(clientFile)) {
						merge.putNextEntry(new ZipEntry(relative.toString()));
						reusedBuffer = writeAll(clientStream, merge, reusedBuffer);
						merge.closeEntry();
					}
				}
			}

		}

		for(Path directory : server.getRootDirectories()) {
			Iterator<Path> files = Files.walk(directory).iterator();
			while(files.hasNext()) {
				Path serverFile = files.next();
				if(Files.isDirectory(serverFile)) continue;
				Path relative = directory.relativize(serverFile);
				if(!Files.exists(client.getPath(relative.toString()))) {
					if(serverFile.toString().endsWith(".class")) {
						reusedBuffer = annotate(handler, merge, reusedBuffer, serverFile, relative, false);
					} else {
						try(InputStream serverStream = Files.newInputStream(serverFile)) {
							merge.putNextEntry(new ZipEntry(relative.toString()));
							reusedBuffer = writeAll(serverStream, merge, reusedBuffer);
							merge.closeEntry();
						}
					}
				}
			}
		}
	}

	private static byte[] annotate(CASMerger.Handler handler, ZipOutputStream merge, byte[] reusedBuffer, Path serverFile, Path relative, boolean isClient)
			throws IOException {
		byte code[];
		try(InputStream clientStream = Files.newInputStream(serverFile)) {
			reusedBuffer = readAll(clientStream, reusedBuffer);
			ClassReader clientReader = new ClassReader(reusedBuffer, 0, reusedBuffer.length); // length is not used
			ClassWriter writer = new ClassWriter(clientReader, 0);
			clientReader.accept(writer, 0);
			handler.accept(writer.visitAnnotation(handler.normalDesc(), false), isClient);
			code = writer.toByteArray();
		}
		merge.putNextEntry(new ZipEntry(relative.toString()));
		merge.write(code);
		merge.closeEntry();
		return reusedBuffer;
	}

	static byte[] writeAll(InputStream stream, OutputStream out, byte[] curr) throws IOException {
		int offset = 0, read;
		while((read = stream.read(curr, offset, curr.length - offset)) != -1) {
			if((read + offset) >= curr.length) { // if buffer is full
				curr = Arrays.copyOf(curr, curr.length << 1);
			}
			offset += read;
		}
		out.write(curr, 0, offset);
		return curr;
	}

	static byte[] readAll(InputStream stream, byte[] curr) throws IOException {
		int offset = 0, read;
		while((read = stream.read(curr, offset, curr.length - offset)) != -1) {
			if((read + offset) >= curr.length) { // if buffer is full
				curr = Arrays.copyOf(curr, curr.length << 1);
			}
			offset += read;
		}
		return curr;
	}
}
