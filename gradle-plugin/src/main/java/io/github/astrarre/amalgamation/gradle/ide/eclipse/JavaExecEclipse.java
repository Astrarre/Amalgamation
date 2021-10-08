package io.github.astrarre.amalgamation.gradle.ide.eclipse;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import org.gradle.api.tasks.JavaExec;

public class JavaExecEclipse extends FileTaskConverter<JavaExec> {
	public JavaExecEclipse(JavaExec task) {
		super(task);
	}

	@Override
	public void emit() {
		//this.task.getClasspath().getFiles()
		//writeTemplate();
	}
}
