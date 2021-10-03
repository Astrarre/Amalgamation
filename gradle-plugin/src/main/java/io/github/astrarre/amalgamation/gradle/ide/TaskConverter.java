package io.github.astrarre.amalgamation.gradle.ide;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.Task;

public abstract class TaskConverter<T extends Task> {
	public final T task;

	public TaskConverter(T task) {
		this.task = task;
	}

	public abstract void emit() throws IOException;

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
}
