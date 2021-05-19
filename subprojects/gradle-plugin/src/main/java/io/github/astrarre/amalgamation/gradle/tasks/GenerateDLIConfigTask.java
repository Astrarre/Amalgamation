package io.github.astrarre.amalgamation.gradle.tasks;

import org.gradle.api.DefaultTask;

public class GenerateDLIConfigTask extends DefaultTask {
	public boolean isDevelopment = true;
	// todo generate GenerateRemapClasspathFileTask



	public enum Sides {
		COMMON,
		CLIENT,
		SERVER
	}

	public enum Args {

	}
}
