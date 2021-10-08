package io.github.astrarre.amalgamation.gradle.ide;

import java.nio.file.Path;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.dependencies.ManifestJarDependency;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;

public abstract class FileTaskConverter<T extends Task> extends NamedTaskConverter<T> {
	public String customPath;
	public FileTaskConverter(T task) {
		super(task);
	}

	@Override
	public void setCustomName(String customName) {
		super.setCustomName(customName);
		this.customPath = customName.replaceAll("[^A-Za-z0-9]", "_");
	}

	public String getCustomPath() {
		return this.customPath;
	}

	public void setCustomPath(String customPath) {
		this.customPath = customPath;
	}


}
