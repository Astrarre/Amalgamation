package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Project;

public class LibrariesDependency2 extends CachedDependency {
	/**
	 * raw bytes of an empty jar file, cry about it
	 */
	private static final byte[] EMPTY_JAR;
	static {
		var s = "UEsDBBQAAAAAALCwI1QAAAAAAAAAAAAAAAAUA" +
		        "AAATUVUQS1JTkYvTUFOSUZFU1QuTUZQSwECFA" +
		        "AUAAAAAACwsCNUAAAAAAAAAAAAAAAAFAAAAAA" +
		        "AAAAAACAAAAAAAAAATUVUQS1JTkYvTUFOSUZF" +
		        "U1QuTUZQSwUGAAAAAAEAAQBCAAAAMgAAAAAA";
		EMPTY_JAR = Base64.getDecoder().decode(s);
	}

	final Path librariesCache;
	final String version;
	LauncherMeta.Version vers;

	public LibrariesDependency2(Project project, String version) {
		super(project);
		this.librariesCache = Path.of(MinecraftAmalgamationGradlePlugin.getLibrariesCache(project));
		this.version = version;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putString(version().manifestUrl, StandardCharsets.UTF_8); // the manifest url has a hashcode inside it, so this should be safe
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.librariesCache
				.resolve("io")
				.resolve("github")
				.resolve("amalgamation")
				.resolve("minecraft-libraries")
				.resolve(this.version)
				.resolve("minecraft-libraries-" + this.version + ".pom");
	}

	public Object getDependencyNotation() {
		return "io.github.amalgamation:minecraft-libraries:" + this.version;
	}

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			StringBuilder template = new StringBuilder("""
					<?xml version="1.0" encoding="UTF-8"?>
					<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
					    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
					\t<!-- This module was also published with a richer model, Gradle metadata,  -->
					\t<!-- which should be used instead. Do not delete the following line which  -->
					\t<!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
					\t<!-- that they should prefer consuming it instead. -->
					\t<!-- do_not_remove: published-with-gradle-metadata -->
					\t<modelVersion>4.0.0</modelVersion>
					\t<groupId>io.github.amalgamation</groupId>
					\t<artifactId>minecraft-libraries</artifactId>
					\t<version>""".stripIndent())
					.append(this.version)
					.append("</version>\n")
					.append("\t<dependencies>\n");
			for(LauncherMeta.Library library : version().getLibraries()) {
				template.append("\t\t<dependency>\n");
				String group = null, name = null, version = null;
				String[] parsed = library.name.split(":");
				if(parsed.length == 1) {
					name = parsed[0];
				} else if(parsed.length == 2) {
					group = parsed[0];
					name = parsed[1];
				} else if(parsed.length == 3) {
					group = parsed[0];
					name = parsed[1];
					version = parsed[2];
				}
				if(group != null) {
					template.append("\t\t\t<groupId>").append(group).append("</groupId>\n");
				}
				template.append("\t\t\t<artifactId>").append(name).append("</artifactId>\n");
				if(version != null) {
					template.append("\t\t\t<version>").append(version).append("</version>\n");
				}
				template.append("\t\t\t<scope>compile</scope>\n");
				template.append("\t\t</dependency>\n");
			}
			template.append("\t</dependencies>\n");
			template.append("</project>");
			Files.write(AmalgIO.changeExtension(resolvedPath, "jar"), EMPTY_JAR);
			Files.writeString(resolvedPath, template.toString());
		}
		return Set.of(); // this is just for caching's sake
	}

	LauncherMeta.Version version() {
		LauncherMeta.Version vers = this.vers;
		if(vers == null) {
			LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project);
			vers = meta.getVersion(this.version);
		}
		return vers;
	}
}
