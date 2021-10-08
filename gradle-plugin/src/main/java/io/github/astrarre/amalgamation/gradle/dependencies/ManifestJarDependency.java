package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.refactor.CachedDependency;
import io.github.astrarre.amalgamation.gradle.ide.TaskConverter;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;

/**
 * An interesting hack, creates a jar file with manifest file with the classpath in it. This allows for really large classpaths
 */
public class ManifestJarDependency extends CachedDependency {
	final String path;
	final List<String> files;

	public ManifestJarDependency(Project project, String path, JavaExec files) {
		super(project, "io.github.astrarre.amalgamation", "manifest", "0");
		this.path = path.replaceAll("[^A-Za-z0-9]", "_");
		this.files = TaskConverter.getClasspath(files);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		for(String file : files) {
			hasher.putString(file, StandardCharsets.UTF_8);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return AmalgIO.projectCache(this.project).resolve("classpath_manifests").resolve(this.path + ".jar");
	}

	@Override
	protected Iterable<Path> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			try(FileSystem write = U.createZip(resolvedPath)) {
				Path path = write.getPath("META-INF/MANIFEST.MF");
				String cp = String.join(" ", this.files);
				Files.writeString(path, String.format("Class-Path: %s", String.join(" ", cp)));
			}
		}
		return List.of(resolvedPath);
	}
}
