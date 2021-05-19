package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import io.github.astrarre.amalgamation.utils.CachedFile;
import org.jetbrains.annotations.Nullable;

public class LibraryStrippedFile extends CachedFile<Long> {
	private final CachedFile<?> serverJar;
	public LibraryStrippedFile(Path file, CachedFile<?> serverJar) {
		super(file, Long.class);
		this.serverJar = serverJar;
	}

	@Nullable
	@Override
	protected Long writeIfOutdated(Path path, @Nullable Long currentData) throws Throwable {
		Path serverJar = this.serverJar.getOutdatedPath();
		if(currentData == null || Files.getLastModifiedTime(serverJar).toMillis() > currentData) {
			Files.copy(serverJar, path);
			try (FileSystem fileSystem = FileSystems.newFileSystem(path, (ClassLoader) null)) {
				Path root = fileSystem.getPath("/");
				Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						String name = root.relativize(file).toString();
						if (!name.startsWith("net/minecraft/") && name.contains("/")) {
							Files.delete(file);
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}
		}
		return null;
	}
}
