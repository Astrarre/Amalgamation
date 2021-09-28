package io.github.astrarre.amalgamation.gradle.files;

import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Project;

public class MinecraftFileHelper {

	public static CachedFile mojmap(Project project, String version, boolean isClient) {
		Path path = AmalgIO.globalCache(project.getGradle()).resolve(version).resolve((isClient ? "client" : "server") + "_mappings.txt");
		var url = forVers(project, version, isClient);
		return CachedFile.forUrl(url, path, project.getLogger(), true);
	}

	public static LauncherMeta.HashedURL forVers(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return isClient ? vers.getClientMojMap() : vers.getServerMojmap();
	}

	public static CachedFile file(Project project, String version, boolean isClient, boolean doStrip) {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project);
		LauncherMeta.Version vers = meta.getVersion(version);
		LauncherMeta.HashedURL url;
		String area;
		if(isClient) {
			url = vers.getClientJar();
			if(doStrip) {
				area = "client";
			} else {
				area = "client-unstripped";
			}
		} else {
			url = vers.getServerJar();
			if(doStrip) {
				area = "server";
			} else {
				area = "server-unstripped";
			}
		}

		Path globalCache = AmalgIO.globalCache(project.getGradle());
		Path jar = globalCache.resolve(version + "-" + area + ".jar");
		Path unstripped = globalCache.resolve(version + "-" + area + ".jar");
		CachedFile file = CachedFile.forUrl(url, unstripped, project.getLogger(), false);
		if(doStrip) {
			return new LibraryStrippedFile(project, jar, file);
		} else {
			return file;
		}
	}
}