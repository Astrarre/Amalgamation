package io.github.astrarre.amalgamation.gradle.ide.eclipse;

import java.io.IOException;

import org.gradle.api.Action;
import org.gradle.api.tasks.JavaExec;

public class EclipseExtension {
	public void java(JavaExec exec, Action<JavaExecEclipse> action) throws IOException {
		JavaExecEclipse eclipse = new JavaExecEclipse(exec);
		action.execute(eclipse);
		eclipse.emit();
	}
}
