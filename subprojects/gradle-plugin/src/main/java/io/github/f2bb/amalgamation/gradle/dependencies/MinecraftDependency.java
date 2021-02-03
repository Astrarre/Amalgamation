package io.github.f2bb.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;
import org.gradle.api.Project;

public class MinecraftDependency extends CachedSelfResolvingDependency {
	public MinecraftDependency(Project project, LauncherMeta meta, String version, Cache cache, boolean isClient) {
		super(project, "net.minecraft", version, isClient ? "client.jar" : "server.jar", cache, (cache1, output) -> {
			LauncherMeta.Version v = Objects.requireNonNull(meta.versions.get(version), "invalid version: " + version);
			if (isClient) {
				return cache1.download(output, new URL(v.getClientJar()));
			} else {
				Path serverJar = cache1.download(output, new URL(v.getServerJar()));
				project.getLogger().lifecycle("getting server without libraries . . .");
				return cache.computeIfAbsent(version + "-stripped-server.jar", sink -> {
					sink.putUnencodedChars("Strip libraries");
					sink.putLong(Files.getLastModifiedTime(serverJar).toMillis());
				}, output2 -> {
					Files.copy(serverJar, output2);
					try (FileSystem fileSystem = FileSystems.newFileSystem(output2, (ClassLoader) null)) {
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
				});
			}
		});
	}
}
