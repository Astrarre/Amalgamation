package io.github.astrarre.amalgamation.gradle.files;

import java.nio.file.Path;
import java.util.function.Supplier;

import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class MojmapFile extends URLCachedFile.Hashed {
	public MojmapFile(Project project, String version, boolean isClient) {
		super(AmalgIO.globalCache(project.getGradle()).resolve(version).resolve((isClient ? "client" : "server") + "_mappings.txt"),
				forVers(project, version, isClient),
				project.getLogger(),
				true);
	}

	static LauncherMeta.HashedURL forVers(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return isClient ? vers.getClientMojMap() : vers.getServerMojmap();
	}

	public MojmapFile(Supplier<Path> file, LauncherMeta.HashedURL url, Logger logger, boolean compress) {
		super(file, url, logger, compress);
	}
}
