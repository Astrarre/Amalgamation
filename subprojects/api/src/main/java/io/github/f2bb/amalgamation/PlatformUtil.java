package io.github.f2bb.amalgamation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;

import io.github.f2bb.amalgamation.forge.ForgeInstalls;
import net.minecraftforge.installer.DownloadUtils;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;
import net.minecraftforge.installer.json.Version;

public class PlatformUtil {
	public static final String MINECRAFT_VERSION;
	public static final String FORGE_VERSION;

	static {
		try {
			Properties properties = new Properties();
			properties.load(PlatformUtil.class.getResourceAsStream("/gradle_data.properties"));
			MINECRAFT_VERSION = properties.getProperty("minecraft_version");
			FORGE_VERSION = properties.getProperty("forge_version");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		getBukkitServer(new File("ohno"));
	}

	/**
	 * @return the unmapped bukkit server jar
	 */
	public static File getBukkitServer(File cache) {
		if (!cache.exists()) {
			cache.mkdirs();
		}

		File buildtools = new File(cache, "buildtools.jar");
		if (!buildtools.exists()) {
			DownloadUtils.downloadFile(buildtools,
					"https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools" + ".jar");
		}

		try {
			URLClassLoader classLoader = new URLClassLoader(new URL[] {buildtools.toURI().toURL()});
			classLoader.loadClass("org.spigotmc.builder.Builder").getMethod("main", String[].class)
			           .invoke(null, (Object) new String[] {
					           "--rev",
					           MINECRAFT_VERSION
			           });
			classLoader.close();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return new File(cache, "spigot-" + MINECRAFT_VERSION + ".jar");
	}

	/**
	 * @param libs the folder for caching libraries
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

	/**
	 * @param server the vanilla server jar
	 * @return the unmapped server forge jar
	 */
	public static File getForgeServer(File libs, File server) {
		installLibs(libs);
		new ForgeInstalls.Server(Util.loadInstallProfile(), (m, p) -> {
			if (p == ProgressCallback.MessagePriority.NORMAL) {
				System.out.println(m);
			}
		}, libs.toPath(), server).run(null, i -> true);
		return new File(libs, String.format("net/minecraftforge/forge/%s/forge-%1$s-server.jar", FORGE_VERSION));
	}
}
