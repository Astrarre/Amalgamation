package io.github.astrarre.amalgamation.gradle.ide.eclipse;

import org.gradle.api.Project;

public class ConfigureEclipse {
	public static EclipseExtension extension;
	public static void configure(Project project) {
		extension = new EclipseExtension();
	}
}
