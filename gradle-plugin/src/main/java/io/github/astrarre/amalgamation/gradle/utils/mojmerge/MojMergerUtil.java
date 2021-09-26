package io.github.astrarre.amalgamation.gradle.utils.mojmerge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

// todo optimize merger to allow skipMembers & interfaces, in 1.17+ fields/methods are no longer stripped
public class MojMergerUtil {
	static final ThreadLocal<Buf> LOCAL = ThreadLocal.withInitial(() -> new Buf(new byte[4096], 0));
	final Path client; final Path serverMappings; final Path merge; final CASMerger.Handler handler;
	final MemoryMappingTree mappingTree = new MemoryMappingTree(true);

	public MojMergerUtil(Path client, Path mappings, Path merge, CASMerger.Handler handler) throws IOException {
		this.client = client;
		this.serverMappings = mappings;
		this.merge = merge;
		this.handler = handler;
		try(Reader reader = Files.newBufferedReader(this.serverMappings)) {
			ProGuardReader.read(reader, this.mappingTree);
		}
	}

	public void merge() throws IOException { // todo zip io optimization
		Files.deleteIfExists(this.merge);
		try(FileSystem input = FileSystems.newFileSystem(this.client, (ClassLoader) null);
		    FileSystem output = FileSystems.newFileSystem(this.merge, NIO_CREATE)) {
			for(Path directory : input.getRootDirectories()) {
				Files.walk(directory).parallel().filter(Files::isRegularFile).forEach(p -> {
					try {
						Buf buffer = LOCAL.get();
						Path relativized = directory.relativize(p);
						Path at = output.getPath(relativized.toString());
						Path dir = at.getParent();
						if(dir != null) {
							Files.createDirectories(dir);
						}
						if(p.toString().endsWith(".class")) {
							try(InputStream i = Files.newInputStream(p)) {
								readAll(i, buffer);
							}
							byte[] o = this.task(buffer.buffer, buffer.len);
							int len = o.length;
							try(OutputStream zos = Files.newOutputStream(at, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
								zos.write(o, 0, len);
							}
						} else {
							Files.copy(p, at);
						}
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	private byte[] task(byte[] buffer, int len) {
		ClassReader clientReader = new ClassReader(buffer, 0, len);
		ClassWriter writer = new ClassWriter(0);
		MojMerger merger = new MojMerger(Opcodes.ASM9, writer, this.handler, this.mappingTree);
		clientReader.accept(merger, 0);
		return writer.toByteArray();
	}

	public static final ImmutableMap<String, ?> NIO_CREATE = ImmutableMap.of("create", "true");
	public static void readAll(InputStream stream, Buf buf) throws IOException {
		int offset = 0, read;
		byte[] curr = buf.buffer;
		while((read = stream.read(curr, offset, curr.length - offset)) != -1) {
			if((read + offset) >= curr.length) { // if buffer is full
				curr = Arrays.copyOf(curr, curr.length * 2);
			}
			offset += read;
		}
		buf.buffer = curr;
		buf.len = offset;
	}
	static final class Buf {
		byte[] buffer;
		int len;

		public Buf(byte[] buffer, int len) {
			this.buffer = buffer;
			this.len = len;
		}
	}
}
