package io.github.f2bb.amalgamation.gradle.extensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.f2bb.amalgamation.gradle.base.BaseAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;
import org.gradle.api.Project;

public class LauncherMeta {
	public static final LauncherMeta EMPTY = new LauncherMeta();
	public static final Version EMPTY_VERSION = EMPTY.new Version(-1, "empty", "empty");
	public final Project project;

	/**
	 * versionName -> Version
	 */
	public final Map<String, Version> versions;

	private final Cache cache;

	private LauncherMeta() {
		this.versions = Collections.emptyMap();
		this.cache = null;
		this.project = null;
	}

	public LauncherMeta(Cache cache, Project project) throws IOException {
		this.cache = cache;

		Map<String, Version> versions = new HashMap<>();
		List<String> orderedVersions = new ArrayList<>();

		this.project = project;

		project.getLogger().lifecycle("downloading manifest . . .");
		JsonObject object = this.read(cache, "version_manifest.json", "https://launchermeta.mojang.com/mc/game/version_manifest.json");

		int index = 0;
		for (JsonElement version : object.getAsJsonArray("versions")) {
			JsonObject obj = (JsonObject) version;
			String versionName = obj.get("id").getAsString();
			String versionJsonURL = obj.get("url").getAsString();
			versions.put(versionName, new Version(index++, versionName, versionJsonURL));
			orderedVersions.add(versionName);
		}
		this.versions = Collections.unmodifiableMap(versions);
	}

	public JsonObject read(Cache cache, String output, String url) {
		try (BufferedReader reader = Files.newBufferedReader(cache.download(output, new URL(url)))) {
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
				JsonObject versionJson = LauncherMeta.this.read(LauncherMeta.this.cache, this.version + "-downloads.json", this.manifestUrl);

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
