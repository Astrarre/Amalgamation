package io.github.astrarre.amalgamation.gradle.dependencies.decomp;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

public class FernflowerIO {}/*implements IResultSaver, IBytecodeProvider, Runnable, Closeable, AutoCloseable {

	private final Map<Path, FileSystem> archives = new ConcurrentHashMap<>();
	private final Object awaiter = new Object();
	private final AtomicBoolean done = new AtomicBoolean(false);
	private Path output;
	
	public FernflowerIO() {
		new Thread(this).start();
	}

	@Override
	public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
		FileSystem zipfs = zipFs(Paths.get(externalPath));
		return Files.readAllBytes(zipfs.getPath(internalPath));
	}

	@Override
	public void saveFolder(String path) {
			Files.createDirectories(Paths.get(path));
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
			Files.copy(Paths.get(source), Paths.get(path, entryName));
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
			Files.writeString(Paths.get(path, entryName), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
			Path archivePath = Paths.get(path, archiveName);
			Files.deleteIfExists(archivePath);
			FileSystem zipfs = zipFs(archivePath);
			if(manifest != null) {
				Path metaInf = zipfs.getPath("META-INF");
				Files.createDirectory(metaInf);
				Path manifestPath = metaInf.resolve("MANIFEST.MF");
				try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(manifestPath, StandardOpenOption.CREATE))) {
					manifest.write(out);
				}
			}
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
			Path archivePath = Paths.get(path, archiveName);
			FileSystem zipfs = zipFs(archivePath);
			Files.createDirectory(zipfs.getPath(entryName));
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
			Path archivePath = Paths.get(path, archiveName);
			FileSystem destFs = zipFs(archivePath);
			FileSystem srcFs = zipFs(Paths.get(source));
			Path destPath = destFs.getPath(entry);
			Path srcPath = srcFs.getPath(entry);
			Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
			Path archivePath = Paths.get(path, archiveName);
			FileSystem zipfs = zipFs(archivePath);
			Files.writeString(zipfs.getPath(entryName), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	@Override
	public void closeArchive(String path, String archiveName) {
		output = Paths.get(path, archiveName).toAbsolutePath();
	}
	
	private FileSystem zipFs(Path path) throws IOException {
		return archives.computeIfAbsent(path.toAbsolutePath(), p -> {
			URI asUri = URI.create("jar:file:" + p.toAbsolutePath().toString());
			try {
				FileSystems.getFileSystem(asUri).close(); // this is a thing bc gradle daemons.
			} catch(FileSystemNotFoundException | IOException e) { }
			try {
				return FileSystems.newFileSystem(asUri, Collections.singletonMap("create", true));
			} catch(IOException e) {
			 	throw U.rethrow(e);
			}
		});
	}

	@Override
	public void run() {
		try {
			ExceptionRunnable<IOException> r;
			while((r = execQueue.take()) != SIGNAL) {
				try {
					r.run();
				} catch(IOException e) {
					System.err.println(String.format("Error during decompilation: %s: %s", e.getClass().getSimpleName(), e.getLocalizedMessage()));
				}
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
		} finally {
			IOException exc = null;
			for(FileSystem fs : archives.values()) {
				try {
					fs.close();
				} catch (IOException e) {
					if(exc == null) {
						exc = e;
					} else {
						exc.addSuppressed(e);
					}
				}
			}
			if(exc != null) {
				throw new RuntimeException("Error(s) closing zip file systems", exc);
			}
		}
		synchronized(awaiter) {
			done.set(true);
			awaiter.notifyAll();
		}
	}
	
	public Path await() {
		synchronized(awaiter) {
			if(!done.get()) {
				try {
					awaiter.wait();
				} catch (InterruptedException e) { }
			}
		}
		return output;
	}

	@Override
	public void close() {
		await();
	}
}*/