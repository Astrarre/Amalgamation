package io.github.astrarre.amalgamation.gradle.ide.util;

public enum CompressCmd {
	/**
	 * Do not shorten the command line
	 */
	DEFAULT,
	/**
	 * Create a dummy jar with the classpath META-INF/Manifest.mf
	 */
	MANIFEST_JAR;

	public enum Idea {
		NONE,
		MANIFEST,
		CLASSPATH_FILE,
		ARGS_FILE
	}
}
