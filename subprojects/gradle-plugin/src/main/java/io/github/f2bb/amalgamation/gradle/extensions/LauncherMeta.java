package io.github.f2bb.amalgamation.gradle.extensions;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.platform.merger.AbstractMergeContext;
import io.github.f2bb.amalgamation.platform.merger.MergeContext;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

public class LauncherMeta {
	/**
	 * versionName -> Version
	 */
	public final Map<String, Version> versions;
	private final Path globalCache;
	private final Logger logger;

	public LauncherMeta(Gradle gradle, Logger logger) {
		this.globalCache = CachedFile.globalCache(gradle);
		this.logger = logger;
		Map<String, Version> versions = new HashMap<>();
		logger.lifecycle("downloading manifest . . .");
		CachedFile<?> cache = CachedFile.forUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json",
				this.globalCache.resolve("version_manifest.json"),
				logger);
		try (Reader reader = cache.getReader()) {
			JsonObject object = BaseAmalgamationGradlePlugin.GSON.fromJson(reader, JsonObject.class);
			int index = 0;
			for (JsonElement version : object.getAsJsonArray("versions")) {
				JsonObject obj = (JsonObject) version;
				String versionName = obj.get("id").getAsString();
				String versionJsonURL = obj.get("url").getAsString();
				versions.put(versionName, new Version(index++, versionName, versionJsonURL));
			}
			this.versions = Collections.unmodifiableMap(versions);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public MergeContext createContext(Iterable<Path> output) {
		return new AbstractMergeContext(output) {
			@Override
			public int versionIndex(String string) {
				LauncherMeta.Version version = LauncherMeta.this.versions.get(string);
				if (version == null) {
					return -1;
				} else {
					return version.index;
				}
			}
		};
	}

	public JsonObject read(String output, String url) {
		CachedFile<?> cache = CachedFile.forUrl(url, this.globalCache.resolve(output), this.logger);
		try (Reader reader = cache.getReader()) {
			return BaseAmalgamationGradlePlugin.GSON.fromJson(reader, JsonObject.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final class Version {
		/**
		 * 0 = latest version, 1 = next latest version, etc.
		 */
		public final int index;
		public final String version, manifestUrl;
		private boolean initialized;
		private String clientJar, serverJar;
		private List<String> libraries;

		public Version(int index, String version, String manifestUrl) {
			this.index = index;
			this.version = Objects.requireNonNull(version, "version");
			this.manifestUrl = Objects.requireNonNull(manifestUrl, "manifestUrl");
		}

		public String getClientJar() {
			this.init();
			return this.clientJar;
		}

		private void init() {
			if (!this.initialized) {
				JsonObject versionJson = LauncherMeta.this.read(this.version + "-downloads.json", this.manifestUrl);
				JsonObject downloads = versionJson.getAsJsonObject("downloads");
				this.clientJar = downloads.getAsJsonObject("client").get("url").getAsString();
				this.serverJar = downloads.getAsJsonObject("server").get("url").getAsString();
				List<String> libraries = new ArrayList<>();
				for (JsonElement element : versionJson.getAsJsonArray("libraries")) {
					libraries.add(element.getAsJsonObject().get("name").getAsString());
				}

				this.libraries = Collections.unmodifiableList(libraries);
				this.initialized = true;
			}
		}

		public String getServerJar() {
			this.init();
			return this.serverJar;
		}

		public List<String> getLibraries() {
			this.init();
			return this.libraries;
		}
	}
}
