package io.github.astrarre.amalgamation.gradle.dependencies.decomp.fernflower;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import io.github.astrarre.amalgamation.gradle.dependencies.decomp.AmalgDecompiler;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

// todo allow for vanilla fernflower
public class AsyncResultSaver implements IResultSaver, AutoCloseable {
	final Map<String, AmalgDecompiler.Entry> entries = new HashMap<>();

	record OutputEntry(FileSystem system, Path linemapPath, StringBuilder lineMap) {}

	/**
	 * input -> outputs
	 */
	final Map<String, OutputEntry> fileSystemCache = new HashMap<>();
	final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

	public AsyncResultSaver(List<AmalgDecompiler.Entry> entries) {
		for(AmalgDecompiler.Entry entry : entries) {
			this.entries.put(entry.input().getFileName().toString(), entry);
		}
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
		try {
			AmalgDecompiler.Entry entry = this.entries.get(archiveName);
			if(entry == null) {
				throw new IOException("Unknown input " + path + "/" + archiveName);
			}
			OutputEntry output = this.fileSystemCache.computeIfAbsent(archiveName, p -> new OutputEntry(AmalgIO.createZip(entry.output()), entry.lineMapOutput(), new StringBuilder()));
			if(manifest != null) {
				FileSystem system = output.system;
				Path manifestPath = system.getPath(JarFile.MANIFEST_NAME);
				Files.createDirectories(manifestPath.getParent());
				try(OutputStream stream = new BufferedOutputStream(Files.newOutputStream(manifestPath))) {
					manifest.write(stream);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
		int[] mapping;
		if (DecompilerContext.getOption(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING)) {
			mapping = DecompilerContext.getBytecodeSourceMapper().getOriginalLinesMapping();
		} else {
			mapping = null;
		}
		this.ioExecutor.submit(() -> {
			OutputEntry entry = this.fileSystemCache.get(archiveName);
			Path output = entry.system.getPath(entryName);
			Files.createDirectories(output.getParent());
			Files.writeString(output, content);
			if (mapping != null && entry.lineMap != null) {
				int maxLine = 0;
				int maxLineDest = 0;
				entry.lineMap
						.append(qualifiedName).append('\t').append(maxLine).append('\t').append(maxLineDest);
				StringBuilder builder = entry.lineMap;
				for (int i = 0; i < mapping.length; i += 2) {
					maxLine = Math.max(maxLine, mapping[i]);
					maxLineDest = Math.max(maxLineDest, mapping[i + 1]);
					builder.append("\t").append(mapping[i]).append("\t").append(mapping[i + 1]).append("\n");
				}
			}
			return null;
		});
	}

	@Override
	public void closeArchive(String path, String archiveName) {
		this.ioExecutor.submit(() -> {
			OutputEntry entry = this.fileSystemCache.get(archiveName);
			entry.system.close();
			if(!entry.lineMap.isEmpty()) {
				Files.createDirectories(entry.linemapPath.getParent());
				Files.writeString(entry.linemapPath, entry.lineMap);
			}
			return null;
		});
	}

	@Override
	public void close() throws Exception {
		this.ioExecutor.shutdown();
	}

	@Override
	public void saveFolder(String path) {
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
	}
}
