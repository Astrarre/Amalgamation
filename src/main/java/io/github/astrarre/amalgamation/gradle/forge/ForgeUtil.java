package io.github.astrarre.amalgamation.gradle.forge;

import java.io.File;
import java.util.ArrayList;


public class ForgeUtil {
	/**
	 * @param libs   the folder for caching libraries
	 * @param client the vanilla client jar
	 */
	public static void getForgeClient(File libs, File client) {
		installLibs(libs);
		/*new ForgeInstalls.Client(Util.loadInstallProfile(), (m, p) -> {
			if (p == ProgressCallback.MessagePriority.NORMAL) {
				System.out.println(m);
			}
		}, libs.toPath(), client).run(null, i -> true);*/
	}

	public static void getForgeServer(File libs, File server) {
		installLibs(libs);
		/*new ForgeInstalls.Server(Util.loadInstallProfile(), (m, p) -> {
			if (p == ProgressCallback.MessagePriority.NORMAL) {
				System.out.println(m);
			}
		}, libs.toPath(), server).run(null, i -> true);*/
	}

	private static void installLibs(File forgeLibs) {
		/*Install profile = Util.loadInstallProfile();
		for (Version.Library lib : profile.getLibraries()) {
			if (!lib.getName().getLocalPath(forgeLibs).exists()) {
				DownloadUtils.downloadLibrary((m, p) -> {
					if (p == ProgressCallback.MessagePriority.NORMAL) {
						System.out.println(m);
					}
				}, profile.getMirror(), lib, forgeLibs, t -> true, new ArrayList<>());
			}
		}*/
	}
}