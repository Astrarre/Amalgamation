package io.github.astrarre.amalgamation.gradle.ide;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.dependencies.ManifestJarDependency;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;

public abstract class TaskConverter<T extends Task> {
	public final T task;

	protected TaskConverter(T task) {this.task = task;}

	public abstract void emit();

	public static void writeTemplate(Path path, String templateLocation, Map<String, String> parameters) throws IOException {
		InputStream templateIn = TaskConverter.class.getResourceAsStream(templateLocation);
		Objects.requireNonNull(templateIn, "no resource found for " + templateLocation);
		String template = new String(templateIn.readAllBytes());
		for(var entry : parameters.entrySet()) {
			template = template.replace(entry.getKey(), entry.getValue());
		}
		Files.createDirectories(path.getParent());
		Files.writeString(path, template);
	}

	protected File getManifestJar(JavaExec task) {
		String path = task.getPath();
		ManifestJarDependency dependency = new ManifestJarDependency(task.getProject(), path, task);
		return Iterables.getOnlyElement(dependency.resolve());
	}

	public static List<String> getClasspath(JavaExec task) {
		return task.getClasspath().getFiles().stream().map(File::getAbsolutePath).toList();
	}
}
