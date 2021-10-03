package io.github.astrarre.amalgamation.gradle.ide.intellij;

import java.io.IOException;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import org.gradle.api.tasks.JavaExec;

public class JavaExecToIntellij extends FileTaskConverter<JavaExec> {
	public JavaExecToIntellij(JavaExec task) {
		super(task);
	}

	@Override
	public void emit() throws IOException {

	}
}
