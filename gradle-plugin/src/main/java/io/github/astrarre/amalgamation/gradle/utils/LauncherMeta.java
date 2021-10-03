package io.github.astrarre.amalgamation.gradle.utils;

import java.io.IOException;
import java.io.Reader;
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.astrarre.amalgamation.gradle.dependencies.refactor.URLDependency;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class LauncherMeta {
	public static final Gson GSON = new Gson();

	private final Path globalCache;
	private final Logger logger;
	private final Project project;
	private Map<String, Version> versions;

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
			default -> throw new UnsupportedOperationException("unsupported operating system " + os);
		};
	}

	public Version getVersion(String version) {
		this.init(version);
		Version version1 = this.versions.get(version);
		if(version1 == null) throw new IllegalArgumentException("Invalid version " + version);
		return version1;
	}

	private void init(String lookingFor) {
		Map<String, Version> vers = this.versions;
		Path path = this.globalCache.resolve("version_manifest.json");
		if (vers == null && Files.exists(path)) {
			URLDependency cache = new URLDependency(this.project, "https://launchermeta.mojang.com/mc/game/version_manifest.json");
			cache.output = path;
			try (Reader reader = Files.newBufferedReader(cache.output); Clock ignore = new Clock("Reading launchermeta %sms", this.logger)) {
				Map<String, Version> versions = this.read(reader);
				if (versions.containsKey(lookingFor)) {
					vers = this.versions = versions;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (vers == null || !vers.containsKey(lookingFor)) {
			URLDependency cache = new URLDependency(this.project, "https://launchermeta.mojang.com/mc/game/version_manifest.json");
			cache.output = path;
			this.logger.lifecycle("Downloading launchermeta...");
			try (Reader reader = cache.getOutdatedReader(); Clock ignore = new Clock("Reading launchermeta %sms", this.logger)) {
				this.versions = this.read(reader);
			} catch (IOException e) {
				throw U.rethrow(e);
			}
		}
	}

	protected Map<String, Version> read(Reader reader) {
		Map<String, Version> versions = new HashMap<>();
		// todo stop using gson, use a visitor based parser since 99% of the time you don't need to parse the entire damn thing
		JsonObject object = GSON.fromJson(reader, JsonObject.class);
		int index = 0;
		for (JsonElement version : object.getAsJsonArray("versions")) {
			JsonObject obj = (JsonObject) version;
			String versionName = obj.get("id").getAsString();
			String versionJsonURL = obj.get("url").getAsString();
			versions.put(versionName, new Version(index++, versionName, versionJsonURL));
		}
		return Collections.unmodifiableMap(versions);
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

		public JsonObject read(String output, String url) {
			// todo pull from .minecraft
			URLDependency dependency = new URLDependency(LauncherMeta.this.project, url);
			dependency.output = AmalgIO.globalCache(LauncherMeta.this.project.getGradle()).resolve(this.version).resolve(output);
			try (Reader reader = dependency.getOutdatedReader()) {
				return GSON.fromJson(reader, JsonObject.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void init() {
			if (!this.initialized) {
				JsonObject versionJson = this.read(this.version + "-downloads.json", this.manifestUrl);
				JsonObject downloads = versionJson.getAsJsonObject("downloads");
				this.clientJar = new HashedURL(downloads.getAsJsonObject("client"), this.version + "-client.jar");
				this.serverJar = new HashedURL(downloads.getAsJsonObject("server"), this.version + "-server.jar");
				this.serverMojmap = new HashedURL(downloads.getAsJsonObject("server_mappings"), this.version + "-client_mappings.txt");
				this.clientMojMap = new HashedURL(downloads.getAsJsonObject("client_mappings"), this.version + "-server_mappings.txt");

				List<Library> libraries = new ArrayList<>();
				for (JsonElement element : versionJson.getAsJsonArray("libraries")) {
					JsonObject object = element.getAsJsonObject();
					JsonObject libraryDownloads = object.getAsJsonObject("downloads");
					HashedURL mainArtifact = new HashedURL(libraryDownloads.getAsJsonObject("artifact"));
					Map<String, HashedURL> classifiers;
					JsonObject classifiersJson = libraryDownloads.getAsJsonObject("classifiers");

					if(classifiersJson != null) {
						classifiers = new HashMap<>();
						for (String s : classifiersJson.keySet()) {
							classifiers.put(s, new HashedURL(classifiersJson.getAsJsonObject(s)));
						}
						classifiers = Collections.unmodifiableMap(classifiers);
					} else classifiers = Collections.emptyMap();

					Map<String, String> natives;
					JsonObject nativesJson = object.getAsJsonObject("natives");
					if(nativesJson != null) {
						natives = new HashMap<>();
						for (String s : nativesJson.keySet()) {
							natives.put(s, nativesJson.getAsJsonPrimitive(s).getAsString());
						}
						natives = Collections.unmodifiableMap(natives);
					} else natives = Collections.emptyMap();

					List<Rule> rules;
					JsonArray rulesJson = object.getAsJsonArray("rules");
					if(rulesJson != null) {
						rules = new ArrayList<>();
						for (JsonElement jsonElement : rulesJson) {
							rules.add(new Rule(jsonElement.getAsJsonObject()));
						}
						rules = Collections.unmodifiableList(rules);
					} else rules = Collections.emptyList();

					Library library = new Library(mainArtifact, rules, classifiers, natives);
					libraries.add(library);
				}

				this.libraries = Collections.unmodifiableList(libraries);
				this.assetIndexPath = versionJson.getAsJsonPrimitive("assets").getAsString();
				this.assetIndexUrl = new HashedURL(versionJson.getAsJsonObject("assetIndex"), this.assetIndexPath + ".json");
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
		public final HashedURL mainDownloadUrl;
		public final List<Rule> rules;
		public final Map<String, HashedURL> classifierUrls;
		public final Map<String, String> nativesOsToClassifier;
		private Map<NativesRule, List<HashedURL>> evaluatedDependencies;

		public Library(HashedURL artifact, List<Rule> rules, Map<String, HashedURL> urls, Map<String, String> classifier) {
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

		public Rule(JsonObject object) {
			this.action = RuleType.valueOf(object.getAsJsonPrimitive("action").getAsString().toUpperCase(Locale.ROOT));
			JsonObject os = object.getAsJsonObject("os");
			if(os == null) {
				this.osName = null;
				return;
			}
			JsonPrimitive primitive = os.getAsJsonPrimitive("name");
			if(primitive == null) {
				this.osName = null;
			} else
				this.osName = primitive.getAsString();
		}

	}

	public enum RuleType {
		ALLOW,
		DISALLOW
	}

	public static final class HashedURL {
		public final String hash, url, path;
		public HashedURL(JsonObject artifact) {
			this.hash = artifact.getAsJsonPrimitive("sha1").getAsString();
			this.url = artifact.getAsJsonPrimitive("url").getAsString();
			this.path = artifact.getAsJsonPrimitive("path").getAsString();
		}

		public HashedURL(JsonObject artifact, String path) {
			this.hash = artifact.getAsJsonPrimitive("sha1").getAsString();
			this.url = artifact.getAsJsonPrimitive("url").getAsString();
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
