package io.github.astrarre.amalgamation.utils;

public class OsUtil {
	public static final String OPERATING_SYSTEM;
	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			OPERATING_SYSTEM = "windows";
		} else if (osName.contains("mac")) {
			OPERATING_SYSTEM = "osx";
		} else {
			OPERATING_SYSTEM = "linux";
		}
	}
}
