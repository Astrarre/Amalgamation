package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.jetbrains.annotations.Nullable;

public abstract class Artifact extends OutputTag {
	public final Project project;
	public final String group, name, version;
	public final byte[] hash;
	public final Type type;

	public Artifact(Project project, String group, String name, String version, Path file, byte[] hash, Type sources) {
		super(file);
		this.project = project;
		this.group = group;
		this.name = name;
		this.version = version;
		this.hash = hash;
		this.type = sources;
	}

	public Artifact(Project project, Dependency dependency, Path file, byte[] hash, Type sources) {
		super(file);
		this.project = project;
		this.group = dependency.getGroup();
		this.name = dependency.getName();
		this.version = dependency.getVersion();
		this.hash = hash;
		this.type = sources;
	}

	public Artifact(Artifact artifact, Path path, byte[] hash) {
		this(artifact.project, artifact.group, artifact.name, artifact.version, path, hash == null ? artifact.hash : hash, artifact.type);
	}

	public void makeGradleFriendly() {}

	public abstract Object toDependencyNotation();

	public abstract Artifact derive(Path newPath, @Nullable byte[] hash);

	public Artifact deriveMaven(Path directory, byte[] hash) {
		return this.deriveMaven(directory, hash, this.type);
	}

	public Artifact deriveMaven(Path directory, byte[] hash, Type type) {
		if(this.type == Type.RESOURCES) {
			return this;
		}
		return new Maven(this.project,
				this.group,
				this.name,
				this.version,
				resolve(directory, this.group, this.name, this.version, type, hash),
				hash,
				type
		);
	}

	public Artifact deriveMavenMixHash(Path directory, byte[] hash) {
		return this.deriveMavenMixHash(directory, hash, this.type);
	}

	public Artifact deriveMavenMixHash(Path directory, byte[] hash, Type type) {
		Hasher hasher = AmalgIO.SHA256.newHasher();
		hasher.putBytes(hash);
		hasher.putBytes(this.hash);
		return this.deriveMaven(directory, hasher.hash().asBytes(), type);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + (this.group != null ? this.group.hashCode() : 0);
		result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(this.hash);
		result = 31 * result + (this.type != null ? this.type.hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(!(o instanceof Artifact artifact)) {
			return false;
		}

		if(!Objects.equals(this.group, artifact.group)) {
			return false;
		}
		if(!Objects.equals(this.name, artifact.name)) {
			return false;
		}
		if(!Objects.equals(this.version, artifact.version)) {
			return false;
		}
		if(!Arrays.equals(this.hash, artifact.hash)) {
			return false;
		}
		return this.type == artifact.type;
	}

	@Override
	public String toString() {
		return group + ":" + name + ":" + version + " " + this.path;
	}

	static Path resolve(Path directory, String group, String name, String version, Type type, byte[] hash) {
		String trueName = name + "_" + version;
		String trueVersion = AmalgIO.b64(hash) + "_" + name.length();
		String file = trueName + "-" + trueVersion;
		if(type == Type.SOURCES) {
			file += "-sources";
		}
		return directory.resolve(group.replace('.', '/')).resolve(trueName).resolve(trueVersion).resolve(file + ".jar");
	}

	public enum Type {
		MIXED(""), CLASSES(""), SOURCES("-sources"), RESOURCES("-resources");

		public final String classifier;

		Type(String classifier) {this.classifier = classifier;}

		public boolean isResources() {
			return this == RESOURCES;
		}

		public boolean containsSources() {
			return this == MIXED || this == SOURCES;
		}

		public boolean containsClasses() {
			return this == MIXED || this == CLASSES;
		}
	}

	public static class File extends Artifact {
		public File(Project project, String group, String name, String version, Path file, byte[] hash, Type sources) {
			super(project, group, name, version, file, hash, sources);
		}

		public File(Project project, Dependency dependency, Path file, byte[] hash, Type sources) {
			super(project, dependency, file, hash, sources);
		}

		public File(Artifact artifact, Path path, byte[] hash) {
			super(artifact, path, hash);
		}

		@Override
		public Object toDependencyNotation() {
			return this.project.files(this.path.toFile());
		}

		@Override
		public Artifact derive(Path newPath, @Nullable byte[] hash) {
			if(this.type == Type.RESOURCES) {
				return this;
			}
			return new File(this, newPath, hash);
		}
	}

	public static class Maven extends Artifact {
		boolean isOutput;

		public Maven(Project project, ComponentArtifactIdentifier id, java.io.File art, String classifier, byte[] hash) {
			super(project,
					get(id, ModuleComponentIdentifier::getGroup, "unknown"),
					get(id, ModuleComponentIdentifier::getModule, id.toString()),
					get(id, ModuleComponentIdentifier::getVersion, "NaN"),
					art.toPath(),
					Objects.requireNonNull(hash, "no hash found for " + id.getComponentIdentifier()),
					Objects.equals(classifier, "sources") ? Type.SOURCES : Type.MIXED
			);
		}

		public Maven(Project project, String group, String name, String version, Path path, byte[] hash, Type sources) {
			super(project, group, name, version, path, hash, sources);
		}

		public Maven(Project project, Dependency dependency, Path path, byte[] hash, Type sources) {
			super(project, dependency, path, hash, sources);
		}

		public Maven(Artifact artifact, Path path, byte[] hash) {
			super(artifact, path, hash);
		}

		boolean isFriendly;

		@Override
		public void makeGradleFriendly() {
			if(isFriendly) {
				return;
			}
			isFriendly = true;
			if(!Files.exists(this.path)) {
				this.project.getLogger().warn("[Amalgamation] artifact's " + this.group + ":" + this.name + ":" + this.version + "'s file does not exist! " +
				                              "This is likely because a file was deleted without deleting it's cache file, " +
				                              "please delete the build/amalgamation cache in the root project! " + this.path);
			}
			String name = this.path.getFileName().toString();
			int ext = name.lastIndexOf('.');
			Path resolved = this.path.getParent().resolve(name.substring(0, ext) + ".pom");
			if(!Files.exists(resolved)) {
				String trueName = this.name + "_" + this.version;
				String trueVersion = AmalgIO.b64(this.hash) + "_" +  this.name.length();
				try {
					U.createDirs(resolved);
					Files.writeString(resolved, String.format("""
							<?xml version="1.0" encoding="UTF-8"?>
							<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
									 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
								<modelVersion>4.0.0</modelVersion>
								<groupId>%s</groupId>
								<artifactId>%s</artifactId>
								<version>%s</version>
							</project>
							""".stripIndent(), this.group, trueName, trueVersion));
				} catch(IOException e) {
					throw U.rethrow(e);
				}
			}
		}

		@Override
		public Object toDependencyNotation() {
			if(isOutput) {
				String str = this.name;
				if(group != null) {
					str = group + ":" + str;
				}
				if(version != null) {
					str = str + ":" + version;
				}
				return str;
			} else {
				return group + ':' + name + '_' + version + ':' + AmalgIO.b64(hash) + "_" + name.length();
			}
		}

		@Override
		public Artifact derive(Path newPath, @Nullable byte[] hash) {
			if(this.type == Type.RESOURCES) {
				return this;
			}
			return new Maven(this, newPath, hash);
		}

		static String get(ComponentArtifactIdentifier id, Function<ModuleComponentIdentifier, String> nameGetter, String default_) {
			if(id instanceof ModuleComponentArtifactIdentifier m) {
				return nameGetter.apply(m.getComponentIdentifier());
			} else {
				return default_;
			}
		}
	}
}
