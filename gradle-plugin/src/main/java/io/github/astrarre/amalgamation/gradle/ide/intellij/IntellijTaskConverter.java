package io.github.astrarre.amalgamation.gradle.ide.intellij;

import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import org.gradle.api.Project;
import org.gradle.api.Task;

public abstract class IntellijTaskConverter<T extends Task> extends FileTaskConverter<T> {
	public boolean buildsWithIntellij;
	public IntellijTaskConverter(T task) {
		super(task);
	}

	public void buildWithIntellij() {
		this.buildsWithIntellij = true;
	}

	public boolean doesBuildWithIntellij() {
		return this.buildsWithIntellij;
	}

	public void setBuildWithIntellij(boolean buildWithIntellij) {
		this.buildsWithIntellij = buildWithIntellij;
	}

	/**
	 * The relative path from the root project to this project
	 */
	public static String pathExtension(Project project) {
		Path root = project.getRootDir().toPath(), dir = project.getProjectDir().toPath();
		Path relative = root.relativize(dir);
		String ext = relative.toString();
		if(ext.isBlank()) {
			return "";
		} else {
			return "/" + ext;
		}
	}

	public static Path getIdeaDir(Project project) {
		return project.getRootDir().toPath().resolve(".idea");
	}
}
