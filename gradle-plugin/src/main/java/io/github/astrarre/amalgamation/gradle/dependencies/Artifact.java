package io.github.astrarre.amalgamation.gradle.dependencies;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.OutputTag;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.impldep.it.unimi.dsi.fastutil.Hash;
import org.jetbrains.annotations.Nullable;

public abstract class Artifact extends OutputTag {
	public final Project project;
	public final String group, name, version;
	public final byte[] hash;
	public final Type type;

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
	}

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

	public abstract Object toDependencyNotation();

	public abstract Artifact derive(Path newPath, @Nullable byte[] hash);

	public abstract Artifact deriveMaven(Path directory, byte[] hash);

	public Artifact deriveMavenMixHash(Path directory, byte[] hash) {
		Hasher hasher = AmalgIO.SHA256.newHasher();
		hasher.putBytes(hash);
		hasher.putBytes(this.hash);
		return this.deriveMaven(directory, hasher.hash().asBytes());
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

		@Override
		public Artifact deriveMaven(Path directory, byte[] hash) {
			if(this.type == Type.RESOURCES) {
				return this;
			}
			return new Maven(this.project, this.group, this.name, this.version, resolve(directory, this.name, this.version, this.type, hash), hash, this.type);
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
					Objects.equals(classifier, "sources") ? Type.SOURCES : Type.MIXED);
		}

		static String get(ComponentArtifactIdentifier id, Function<ModuleComponentIdentifier, String> nameGetter, String default_) {
			if(id instanceof ModuleComponentArtifactIdentifier m) {
				return nameGetter.apply(m.getComponentIdentifier());
			} else {
				return default_;
			}
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

		@Override
		public Artifact deriveMaven(Path directory, byte[] hash) {
			if(this.type == Type.RESOURCES) {
				return this;
			}
			return new Maven(this.project, this.group, this.name, this.version, resolve(directory, this.name, this.version, this.type, hash), hash, this.type);
		}
	}

	static Path resolve(Path directory, String name, String version, Type type, byte[] hash) {
		String file = name + "_" + version + "-" + AmalgIO.b64(hash) + "_" + name.length();
		if(type == Type.SOURCES) {
			file += "-sources";
		}
		return directory.resolve(file + ".jar");
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
	public int hashCode() {
		int result = 0;
		result = 31 * result + (this.group != null ? this.group.hashCode() : 0);
		result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(this.hash);
		result = 31 * result + (this.type != null ? this.type.hashCode() : 0);
		return result;
	}
}
