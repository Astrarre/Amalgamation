/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.astrarre.amalgamation.api.platform.util;

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

	public static void main(String[] args)
			throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		getSpigotServerJar(new File("ohno"));
	}

	/**
	 * todo craftbukkit
	 *
	 * @return the unmapped spigot server jar
	 */
	public static File getSpigotServerJar(File cache)
			throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		if (!cache.exists()) {
			cache.mkdirs();
		}

		File buildtools = new File(cache, "buildtools.jar");
		if (!buildtools.exists()) {
			DownloadUtils.downloadFile(buildtools,
					"https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools" + ".jar");
		}

		URLClassLoader classLoader = new URLClassLoader(new URL[]{buildtools.toURI().toURL()}, null);
		Class<?> builder = Class.forName("org.spigotmc.builder.Builder", true, classLoader);
		Method main = builder.getMethod("main", String[].class);
		String[] args = {
				"--rev",
				MINECRAFT_VERSION,
				"--output-dir",
				"out"
		};
		main.invoke(null, (Object) args);
		return new File(cache, "out/spigot-" + MINECRAFT_VERSION + ".jar");
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
