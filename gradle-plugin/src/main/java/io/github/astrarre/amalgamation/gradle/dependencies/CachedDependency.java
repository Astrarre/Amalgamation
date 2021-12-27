package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public abstract class CachedDependency extends AmalgamationDependency {
	public static final HashFunction HASHING = AmalgIO.SHA256;
	public static final int BYTES = 32; // should ideally remain this way and never change
	@Nullable protected byte[] oldHash, currentHash;
	boolean initOldHash;
	/**
	 * 0 == unevaluated 1 == false 2 == true
	 */
	protected byte isOutdated;
	private Path cachePath, realPath;
	public CachedDependency(Project project) {
		super(project);
	}

	public void hashDep(Hasher hasher, Object notation) throws IOException {
		if(notation instanceof CachedDependency c) {
			c.hashInputs(hasher);
		} else if(notation instanceof Dependency d) {
			AmalgIO.hashDep(hasher, this.project, d);
		} else {
			var resolved = this.project.getDependencies().create(notation);
			AmalgIO.hashDep(hasher, this.project, resolved);
		}
	}

	public boolean isOutdated() {
		byte val = this.isOutdated;
		if(val == 0) {
			try {
				this.getCurrentHash();
				this.initOldHash();
				if(this.oldHash == null || !Arrays.equals(this.oldHash, this.currentHash) || BaseAmalgamationGradlePlugin.refreshAmalgamationCaches) {
					this.isOutdated = val = 2;
				} else {
					this.isOutdated = val = 1;
				}
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		return val == 2;
	}

	public abstract void hashInputs(Hasher hasher) throws IOException;

	/**
	 * This path does not have to actually contain any files, filename.extension.data stores the information on this notation
	 */
	protected abstract Path evaluatePath(byte[] hash) throws IOException;

	protected void readInputs(@Nullable InputStream stream) throws IOException {}

	protected void writeOutputs(OutputStream stream) throws IOException {}

	public Path getPath() {
		if(this.realPath == null) {
			try {
				Path record = this.realPath = this.evaluatePath(this.getCurrentHash());
				Files.createDirectories(record.getParent());
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
		return this.realPath;
	}

	protected Path getCachePath() {
		if(this.cachePath == null) {
			Path record = this.getPath();
			this.cachePath = record.getParent().resolve(record.getFileName() + ".data");
		}
		return this.cachePath;
	}

	protected void initOldHash() throws IOException {
		if(this.initOldHash) {
			return;
		}
		this.initOldHash = true;

		Path realPath = this.getPath();
		Path path = this.getCachePath();

		if(Files.exists(path)) {
			try(InputStream stream = Files.newInputStream(path)) {
				byte[] hash = new byte[BYTES];
				if(stream.read(hash) != BYTES) {
					throw new IOException("Unexpected EOF when reading hash from file!");
				}
				this.oldHash = hash;
				this.readInputs(stream);
				return;
			} catch(IOException e) {
				e.printStackTrace();
				this.logger.lifecycle("Cache hash resolved with error, uncaching file!");
				Files.delete(path);
			}
		}
		this.oldHash = null;
		this.readInputs(null);
	}

	protected byte[] getCurrentHash() {
		byte[] current = this.currentHash;
		if(current == null) {
			Hasher hasher = HASHING.newHasher();
			try {
				this.hashInputs(hasher);
			} catch(IOException e) {
				throw U.rethrow(e);
			}
			byte[] hash = hasher.hash().asBytes();
			if(hash.length != BYTES) {
				hash = Arrays.copyOf(hash, BYTES);
			}
			this.currentHash = hash;
			return hash;
		}
		return current;
	}

	protected abstract Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException;

	public void writeHash() throws IOException {
		byte[] hash = this.getCurrentHash();
		try(OutputStream stream = Files.newOutputStream(this.getCachePath())) {
			stream.write(hash, 0, BYTES);
			this.writeOutputs(stream);
		}
		this.oldHash = hash;
		this.isOutdated = 1;
	}

	@Override
	protected Set<Artifact> resolveArtifacts() throws IOException {
		boolean isOutdated = this.isOutdated();
		Path path = this.getPath();
		Set<Artifact> paths;
		try {
			paths = this.resolve0(path, isOutdated);
		} catch(Uncached u) {
			paths = u.paths;
			isOutdated = false;
		}
		if(isOutdated) {
			this.writeHash();
		}
		return paths;
	}

	protected static final class Uncached extends RuntimeException {
		final Set<Artifact> paths;

		public Uncached(Set<Artifact> paths) {this.paths = paths;}
	}
}
