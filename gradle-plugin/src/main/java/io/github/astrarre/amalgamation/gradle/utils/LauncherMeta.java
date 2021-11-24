package io.github.astrarre.amalgamation.gradle.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import io.github.astrarre.amalgamation.gradle.dependencies.URLDependency;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.json.Json;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class LauncherMeta {
	public static final Gson GSON = new Gson();

	private final Path globalCache;
	private final Logger logger;
	private final Project project;
	private Map<String, Version> versions = new HashMap<>();
	private Json.Obj launcherMeta;

	/**
	 * @see MinecraftAmalgamationGradlePlugin#apply(Project)
	 */
	public LauncherMeta(Path globalCache, Project project) {
		this.globalCache = globalCache;
		this.project = project;
		this.logger = project.getLogger();
	}

	public static String activeMinecraftDirectory() {
		return minecraftDirectory(OS.ACTIVE);
	}

	public static String minecraftDirectory(OS os) {
		return switch(os) {
			case WINDOWS -> System.getenv("appdata") + "/.minecraft";
			case LINUX -> System.getProperty("user.home") + "/.minecraft";
			case MACOS -> System.getProperty("user.home") + "/Library/Application Support/minecraft";
		};
	}

	public Version getVersion(String version) {
		this.init(version);
		Version version1 = this.findVersion(version);
		if(version1 == null) throw new IllegalArgumentException("Invalid version " + version);
		return version1;
	}

	private void init(String lookingFor) {
		Json.Obj vers = this.launcherMeta;
		Path path = this.globalCache.resolve("version_manifest.json");
		if (vers == null && Files.exists(path)) { // if we already have launchermeta downloaded, use that
			URLDependency cache = new URLDependency(this.project, "https://launchermeta.mojang.com/mc/game/version_manifest.json");
			cache.output = path;
			try (Clock ignore = new Clock("Reading launchermeta %sms", this.logger)) {
				Json.Obj versions = new Json.Obj(Files.readString(cache.resolve1()), 0);
				this.launcherMeta = versions;
				if (findVersion(lookingFor) != null) {
					vers = versions;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (vers == null) { // if we don't, download it
			URLDependency cache = new URLDependency(this.project, "https://launchermeta.mojang.com/mc/game/version_manifest.json");
			cache.output = path;
			this.logger.lifecycle("Downloading launchermeta...");
			try (Clock ignore = new Clock("Reading launchermeta %sms", this.logger)) {
				this.launcherMeta = new Json.Obj(Files.readString(cache.resolve1()), 0);
			} catch (IOException e) {
				throw U.rethrow(e);
			}
		}
	}

	private Version findVersion(String version) {
		Json.List versions = this.launcherMeta.getList("versions");
		List<Version> toAdd = new ArrayList<>();
		Version ver = this.versions.computeIfAbsent(version, id -> {
			int i = versions.currentlyParsed();
			while(!versions.hasReachedEnd()) {
				Json.Obj vers = (Json.Obj) versions.get(i);
				String versionName = vers.getString("id");
				String versionJsonURL = vers.getString("url");
				Version value = new Version(i++, versionName, versionJsonURL);
				if(versionName.equals(id)) {
					return value;
				} else {
					toAdd.add(value);
				}
			}
			return null;
		});
		toAdd.forEach(v -> this.versions.put(v.version, v));
		return ver;
	}

	public final class Version {
		/**
		 * 0 = latest version, 1 = next latest version, etc.
		 */
		public final int index;
		public final String version, manifestUrl;
		private boolean initialized;
		private HashedURL assetIndexUrl;
		private String assetIndexPath;
		private HashedURL clientJar, serverJar, serverMojmap, clientMojMap;
		private List<Library> libraries;

		public Version(int index, String version, String manifestUrl) {
			this.index = index;
			this.version = Objects.requireNonNull(version, "version");
			this.manifestUrl = Objects.requireNonNull(manifestUrl, "manifestUrl");
		}

		public HashedURL getClientJar() {
			this.init();
			return this.clientJar;
		}

		public HashedURL getAssetIndexUrl() {
			this.init();
			return this.assetIndexUrl;
		}

		public String getAssetIndexVersion() {
			this.init();
			return this.assetIndexPath;
		}

		public HashedURL getJar(boolean client) {
			return client ? getClientJar() : getServerJar();
		}

		public Json.Obj read(String output, String url) {
			// todo pull from .minecraft
			URLDependency dependency = new URLDependency(LauncherMeta.this.project, url);
			dependency.output = AmalgIO.globalCache(LauncherMeta.this.project).resolve(this.version).resolve(output);
			try (BufferedReader reader = dependency.getOutdatedReader()) {
				String collect = reader.lines().collect(Collectors.joining("\n"));
				return (Json.Obj) Json.parseValue(collect, 0);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void init() {
			if (!this.initialized) {
				Json.Obj versionJson = this.read(this.version + "-downloads.json", this.manifestUrl);
				Json.Obj downloads = versionJson.getObj("downloads");
				this.clientJar = new HashedURL(downloads.getObj("client"), this.version + "-client.jar");
				this.serverJar = new HashedURL(downloads.getObj("server"), this.version + "-server.jar");
				this.serverMojmap = new HashedURL(downloads.getObj("server_mappings"), this.version + "-client_mappings.txt");
				this.clientMojMap = new HashedURL(downloads.getObj("client_mappings"), this.version + "-server_mappings.txt");

				List<Library> libraries = new ArrayList<>();
				for (Json element : versionJson.getList("libraries").asList()) {
					Json.Obj object = (Json.Obj) element;
					String name = object.getString("name");
					Json.Obj libraryDownloads = object.getObj("downloads");
					HashedURL mainArtifact = new HashedURL(libraryDownloads.getObj("artifact"));
					Map<String, HashedURL> classifiers;
					Json.Obj classifiersJson = libraryDownloads.getObj("classifiers");

					if(classifiersJson != null) {
						classifiers = new HashMap<>();
						for (var s : classifiersJson.asMap().entrySet()) {
							classifiers.put(s.getKey(), new HashedURL((Json.Obj)s.getValue()));
						}
						classifiers = Collections.unmodifiableMap(classifiers);
					} else classifiers = Collections.emptyMap();

					Map<String, String> natives;
					Json.Obj nativesJson = object.getObj("natives");
					if(nativesJson != null) {
						natives = new HashMap<>();
						for (var s : nativesJson.asMap().entrySet()) {
							natives.put(s.getKey(), ((Json.Str)s.getValue()).getValue());
						}
						natives = Collections.unmodifiableMap(natives);
					} else natives = Collections.emptyMap();

					List<Rule> rules;
					Json.List rulesJson = object.getList("rules");
					if(rulesJson != null) {
						rules = new ArrayList<>();
						for (Json jsonElement : rulesJson.asList()) {
							rules.add(new Rule((Json.Obj) jsonElement));
						}
						rules = Collections.unmodifiableList(rules);
					} else rules = Collections.emptyList();

					Library library = new Library(name, mainArtifact, rules, classifiers, natives);
					libraries.add(library);
				}

				this.libraries = Collections.unmodifiableList(libraries);
				this.assetIndexPath = versionJson.getString("assets");
				this.assetIndexUrl = new HashedURL(versionJson.getObj("assetIndex"), this.assetIndexPath + ".json");
				this.initialized = true;
			}
		}

		public HashedURL getServerMojmap() {
			this.init();
			return this.serverMojmap;
		}

		public HashedURL getClientMojMap() {
			this.init();
			return this.clientMojMap;
		}

		public HashedURL getServerJar() {
			this.init();
			return this.serverJar;
		}

		public List<Library> getLibraries() {
			this.init();
			return this.libraries;
		}
	}

	public static final class Library {
		public final String name;
		public final HashedURL mainDownloadUrl;
		public final List<Rule> rules;
		public final Map<String, HashedURL> classifierUrls;
		public final Map<String, String> nativesOsToClassifier;
		private Map<NativesRule, List<HashedURL>> evaluatedDependencies;

		public Library(String name, HashedURL artifact, List<Rule> rules, Map<String, HashedURL> urls, Map<String, String> classifier) {
			this.name = name;
			this.mainDownloadUrl = artifact;
			this.rules = rules;
			this.classifierUrls = urls;
			this.nativesOsToClassifier = classifier;
		}

		public List<HashedURL> evaluateAllDependencies(NativesRule natives) {
			List<HashedURL> get = this.evaluatedDependencies != null ? this.evaluatedDependencies.get(natives) : null;
			if(get == null) {
				List<HashedURL> urls = new ArrayList<>();
				if(natives.getNormalDependencies()) {
					boolean includeMain = true;
					for (Rule rule : this.rules) {
						if (rule.action == RuleType.ALLOW) {
							if (rule.osName == null) {
								includeMain = true;
							} else if (OS.ACTIVE.launchermetaName.equals(rule.osName)) {
								includeMain = true;
								break;
							} else {
								includeMain = false;
							}
						} else {
							if (rule.osName == null) {
								includeMain = false;
							} else if (OS.ACTIVE.launchermetaName.equals(rule.osName)) {
								includeMain = false;
								break;
							} else {
								includeMain = true;
							}
						}
					}

					if (includeMain) {
						urls.add(this.mainDownloadUrl);
					}
				}

				if(natives.getNatives()) {
					String classifier = this.nativesOsToClassifier.get(OS.ACTIVE.launchermetaName);
					if (classifier != null) {
						HashedURL url = this.classifierUrls.get(classifier);
						if (url != null) {
							urls.add(url);
						}
					}
				}
				if(this.evaluatedDependencies == null) this.evaluatedDependencies = new EnumMap<>(NativesRule.class);
				this.evaluatedDependencies.put(natives, get = Collections.unmodifiableList(urls));
			}
			return get;
		}

		public String getName() {
			return this.name;
		}
	}

	public enum NativesRule {
		ALL,
		NATIVES_ONLY,
		ALL_NON_NATIVES;

		public boolean getNormalDependencies() {
			return this != NATIVES_ONLY;
		}

		public boolean getNatives() {
			return this != ALL_NON_NATIVES;
		}
	}

	public static class Rule {
		public final RuleType action;
		public final String osName;
		public Rule(RuleType action, String name) {
			this.action = action;
			this.osName = name;
		}

		public Rule(Json.Obj object) {
			this.action = RuleType.valueOf(object.getString("action").toUpperCase(Locale.ROOT));
			Json.Obj os = object.getObj("os");
			if(os == null) {
				this.osName = null;
				return;
			}
			this.osName = os.getString("name");
		}

	}

	public enum RuleType {
		ALLOW,
		DISALLOW
	}

	public static final class HashedURL {
		public final String hash, url, path;
		public HashedURL(Json.Obj artifact) {
			this.hash = artifact.getString("sha1");
			this.url = artifact.getString("url");
			this.path = artifact.getString("path");
		}

		public HashedURL(Json.Obj artifact, String path) {
			this.hash = artifact.getString("sha1");
			this.url = artifact.getString("url");
			this.path = path;
		}

		public HashedURL(String hash, String url, String path) {
			this.hash = hash;
			this.url = url;
			this.path = path;
		}

		public URL getUrl() {
			try {
				return new URL(this.url);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return "hashed " + this.url;
		}
	}
}
