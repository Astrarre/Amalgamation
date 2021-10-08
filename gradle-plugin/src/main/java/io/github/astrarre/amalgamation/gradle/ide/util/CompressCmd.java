package io.github.astrarre.amalgamation.gradle.ide.util;

public enum CompressCmd {
	/**
	 * Do not shorten the command line
	 */
	NONE,
	/**
	 * Create a dummy jar with the classpath META-INF/Manifest.mf
	 */
	MANIFEST_JAR;
}
