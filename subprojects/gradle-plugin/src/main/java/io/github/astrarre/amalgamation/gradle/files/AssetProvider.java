package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.Assets;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.utils.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.OS;
import org.jetbrains.annotations.Nullable;

public class AssetProvider { // todo log config
	@Nullable
	public static Assets getAssetsDir(MinecraftAmalgamationImpl amalgamation, String version) throws IOException {
		amalgamation.logger.lifecycle("downloading assets . . .");
		String assetsDirPath = LauncherMeta.minecraftDirectory(OS.ACTIVE) + "/assets";
		Path assetsDir = Paths.get(assetsDirPath);
		if(Files.exists(assetsDir)) {
			amalgamation.logger.lifecycle("Found .minecraft assets folder");
		} else {
			amalgamation.logger.lifecycle("No .minecraft assets folder, using global cache!");
			assetsDir = BaseAmalgamationImpl.globalCache(amalgamation.project.getGradle()).resolve("assetsDir");
		}

		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(amalgamation.project).getVersion(version);
		String assetIndexVersion = vers.getAssetIndexVersion();
		Path indexFile = assetsDir.resolve("indexes").resolve(vers.getAssetIndexVersion() + ".json");
		Assets assets = new Assets(assetIndexVersion, assetsDirPath);
		if(Files.exists(indexFile) && !BaseAmalgamationGradlePlugin.refreshAmalgamationCaches) {
			return assets;
		}

		CachedFile<?> file = CachedFile.forUrl(vers.getAssetIndexUrl(), indexFile, amalgamation.logger, true);
		Path objectsDir = assetsDir.resolve("objects");
		try(Reader reader = file.getReader()) {
			JsonObject assetsJson = CachedFile.GSON.fromJson(reader, JsonObject.class);
			JsonObject objects = assetsJson.getAsJsonObject("objects");
			List<Future<?>> futures = new ArrayList<>(objects.entrySet().size());
			for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
				Future<?> future = BaseAmalgamationImpl.SERVICE.submit(() -> {
					JsonObject assetJson = entry.getValue().getAsJsonObject();
					String hash = assetJson.getAsJsonPrimitive("hash").getAsString();
					String minHash = hash.substring(0, 2);
					LauncherMeta.HashedURL url = new LauncherMeta.HashedURL(hash,
							"https://resources.download.minecraft.net/" + minHash + "/" + hash,
							entry.getKey());
					// todo maybe not compress for PNG?
					CachedFile<?> asset = CachedFile.forUrl(url, objectsDir.resolve(minHash).resolve(hash), null, true);
					asset.getOutdatedPath();
				});
				futures.add(future);
			}
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (IOException | InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		return assets;
	}
}
