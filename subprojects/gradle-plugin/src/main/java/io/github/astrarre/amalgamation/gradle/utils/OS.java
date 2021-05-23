package io.github.astrarre.amalgamation.gradle.utils;

/**
 * an enum of supported OSes for launchermeta
 */
public enum OS {
	WINDOWS("win", "windows"),
	LINUX("linux", "linux"),
	MACOS("osx", "mac");
	public static final OS ACTIVE;
	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			ACTIVE = WINDOWS;
		} else if (osName.contains("mac")) {
			ACTIVE = MACOS;
		} else {
			ACTIVE = LINUX;
		}
	}

	public final String osName, launchermetaName;

	OS(String osName, String launchermetaName) {
		this.osName = osName;
		this.launchermetaName = launchermetaName;
	}
}
