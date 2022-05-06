package io.github.astrarre.amalgamation.gradle.dependencies;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.filepipeline.api.VirtualDirectory;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.jetbrains.annotations.Nullable;

// todo maybe this should be file to force it to be gradle friendly
public abstract class Artifact {
	public final Project project;
	public final String group, name, version;
	public final byte[] hash;
	public final Type type;
	public final VirtualPath file;
	
	public Artifact(Project project, String group, String name, String version, VirtualPath file, byte[] hash, Type sources) {
		this.file = file;
		this.project = project;
		this.group = group;
		this.name = name;
		this.version = version;
		this.hash = hash;
		this.type = sources;
	}
	
	public Artifact(Project project, Dependency dependency, VirtualPath file, byte[] hash, Type sources) {
		this.file = file;
		this.project = project;
		this.group = dependency.getGroup();
		this.name = dependency.getName();
		this.version = dependency.getVersion();
		this.hash = hash;
		this.type = sources;
	}
	
	public Artifact(Artifact artifact, VirtualPath path, byte[] hash) {
		this(artifact.project, artifact.group, artifact.name, artifact.version, path, hash == null ? artifact.hash : hash, artifact.type);
	}
	
	public void makeGradleFriendly() {}
	
	public abstract Object toDependencyNotation();
	
	public abstract Artifact derive(VirtualPath newPath, @Nullable byte[] hash);
	
	public Artifact deriveMaven(VirtualDirectory directory, byte[] hash) {
		return this.deriveMaven(directory, hash, this.type);
	}
	
	public Artifact deriveMaven(VirtualDirectory directory, byte[] hash, Type type) {
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
	
	public Artifact deriveMavenMixHash(VirtualDirectory directory, byte[] hash) {
		return this.deriveMavenMixHash(directory, hash, this.type);
	}
	
	public Artifact deriveMavenMixHash(VirtualDirectory directory, byte[] hash, Type type) {
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
		return group + ":" + name + ":" + version + " " + this.file;
	}
	
	static VirtualFile resolve(VirtualDirectory directory, String group, String name, String version, Type type, byte[] hash) {
		String trueName = name + "_" + version;
		String trueVersion = AmalgIO.b64(hash) + "_" + name.length();
		String file = trueName + "-" + trueVersion;
		if(type == Type.SOURCES) {
			file += "-sources";
		}
		return directory
				.resolveDirectory(group.replace('.', '/'))
				.resolveDirectory(trueName)
				.resolveDirectory(trueVersion)
				.resolveFile(file + ".jar");
	}
	
	public enum Type {
		MIXED(""),
		CLASSES(""),
		SOURCES("-sources"),
		RESOURCES("-resources");
		
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
		public File(Project project, String group, String name, String version, VirtualPath file, byte[] hash, Type sources) {
			super(project, group, name, version, file, hash, sources);
		}
		
		public File(Project project, Dependency dependency, VirtualPath file, byte[] hash, Type sources) {
			super(project, dependency, file, hash, sources);
		}
		
		public File(Artifact artifact, VirtualPath path, byte[] hash) {
			super(artifact, path, hash);
		}
		
		@Override
		public Object toDependencyNotation() {
			return this.project.files(this.file.relativePath());
		}
		
		@Override
		public Artifact derive(VirtualPath newPath, @Nullable byte[] hash) {
			if(this.type == Type.RESOURCES) {
				return this;
			}
			return new File(this, newPath, hash);
		}
	}
	
	public static class Maven extends Artifact {
		boolean isOutput;
		boolean isFriendly;
		
		public Maven(Project project, ComponentArtifactIdentifier id, java.io.File art, String classifier, byte[] hash) {
			super(project,
					get(id, ModuleComponentIdentifier::getGroup, "unknown"),
					get(id, ModuleComponentIdentifier::getModule, id.toString()),
					get(id, ModuleComponentIdentifier::getVersion, "NaN"),
					AmalgIO.resolve(art.toPath()),
					Objects.requireNonNull(hash, "no hash found for " + id.getComponentIdentifier()),
					Objects.equals(classifier, "sources") ? Type.SOURCES : Type.MIXED
			);
		}
		
		public Maven(Project project, String group, String name, String version, VirtualPath path, byte[] hash, Type sources) {
			super(project, group, name, version, path, hash, sources);
		}
		
		public Maven(Project project, Dependency dependency, VirtualPath path, byte[] hash, Type sources) {
			super(project, dependency, path, hash, sources);
		}
		
		public Maven(Artifact artifact, VirtualPath path, byte[] hash) {
			super(artifact, path, hash);
		}
		
		@Override
		public void makeGradleFriendly() {
			if(isFriendly) {
				return;
			}
			isFriendly = true;
			if(!this.file.exists()) {
				this.project.getLogger().warn(String.format(
						"[Amalgamation] artifact (%s:%s:%s)'s file does not exist! This is likely because a file was deleted without deleting its "
						+ "cache file, please delete the build/amalgamation cache in the root project! (%s)",
						this.group,
						this.name,
						this.version,
						this.file.relativePath()
				));
			}
			String name = this.file.fileName();
			int ext = name.lastIndexOf('.');
			VirtualDirectory parent = Objects.requireNonNull(this.file.getParent(), "what");
			VirtualFile resolved = parent.getFile(name.substring(0, ext) + ".pom");
			if(!resolved.exists()) {
				String trueName = this.name + "_" + this.version;
				String trueVersion = AmalgIO.b64(this.hash) + "_" + this.name.length();
				AmalgIO.DISK_OUT.writeString(resolved, String.format("""
						<?xml version="1.0" encoding="UTF-8"?>
						<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
								 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
							<modelVersion>4.0.0</modelVersion>
							<groupId>%s</groupId>
							<artifactId>%s</artifactId>
							<version>%s</version>
						</project>
						""".stripIndent(), this.group, trueName, trueVersion), StandardCharsets.UTF_8);
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
		public Artifact derive(VirtualPath newPath, @Nullable byte[] hash) {
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
