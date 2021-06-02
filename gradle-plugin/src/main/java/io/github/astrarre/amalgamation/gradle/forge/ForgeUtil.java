package io.github.astrarre.amalgamation.gradle.forge;

import net.minecraftforge.installer.DownloadUtils;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;
import net.minecraftforge.installer.json.Version;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;

public class ForgeUtil {
	public static final String MINECRAFT_VERSION;
	public static final String FORGE_VERSION;

	static {
		try {
			Properties properties = new Properties();
			properties.load(ForgeUtil.class.getResourceAsStream("/gradle_data.properties"));
			MINECRAFT_VERSION = properties.getProperty("minecraft_version");
			FORGE_VERSION = properties.getProperty("forge_version");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param libs   the folder for caching libraries
	 * @param client the vanilla client jar
	 * @return the unmapped client forge jar
	 */
	public static File getForgeClient(File libs, File client) {
		installLibs(libs);
		new ForgeInstalls.Client(Util.loadInstallProfile(), (m, p) -> {
			if (p == ProgressCallback.MessagePriority.NORMAL) {
				System.out.println(m);
			}
		}, libs.toPath(), client).run(null, i -> true);
		return new File(libs, String.format("net/minecraftforge/forge/%s/forge-%1$s-client.jar", FORGE_VERSION));
	}

	private static void installLibs(File forgeLibs) {
		Install profile = Util.loadInstallProfile();
		for (Version.Library lib : profile.getLibraries()) {
			if (!lib.getName().getLocalPath(forgeLibs).exists()) {
				DownloadUtils.downloadLibrary((m, p) -> {
					if (p == ProgressCallback.MessagePriority.NORMAL) {
						System.out.println(m);
					}
				}, profile.getMirror(), lib, forgeLibs, t -> true, new ArrayList<>());
			}
		}
	}
}