package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public abstract class CachedDependency extends AbstractSelfResolvingDependency {
	public static final HashFunction HASHING = Hashing.sha256();
	public static final int BYTES = 32; // should ideally remain this way and never change
	@Nullable protected byte[] oldHash, currentHash;
	boolean initOldHash;
	/**
	 * 0 == unevaluated 1 == false 2 == true
	 */
	protected byte isOutdated;
	private Path cachePath, realPath;
	public CachedDependency(Project project, String group, String name, String version) {
		super(project, group, name, version);
	}

	public Dependency of(Object notation) {
		if(notation instanceof Dependency d) {
			return d;
		} else {
			return this.project.getDependencies().create(notation);
		}
	}

	public Dependency of(Object notation, Closure<ModuleDependency> config) {
		return this.project.getDependencies().create(notation, config);
	}

	public Dependency hashDep(Hasher hasher, Object dependency) throws IOException {
		Dependency resolved = this.of(dependency);
		if(dependency instanceof CachedDependency c) {
			c.hashInputs(hasher);
		} else {
			AmalgIO.hash(hasher, AmalgIO.resolve(this.project, List.of(resolved)));
		}
		return resolved;
	}

	public boolean isOutdated() {
		byte val = this.isOutdated;
		if(val == 0) {
			try {
				this.getCurrentHash();
				this.initOldHash();
				if(this.oldHash == null || !Arrays.equals(this.oldHash, this.currentHash)) {
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


	@Override
	public Dependency copy() {
		throw new UnsupportedOperationException("// TODO: implement");
	}

	public abstract void hashInputs(Hasher hasher) throws IOException;

	/**
	 * This path does not have to actually contain any files, filename.extension.data stores the information on this dependency
	 */
	protected abstract Path evaluatePath(byte[] hash) throws IOException;

	protected void readInputs(@Nullable InputStream stream) throws IOException {}

	protected void writeOutputs(OutputStream stream) throws IOException {}

	public Path getPath() {
		if(this.realPath == null) {
			try {
				Path record = this.realPath = this.evaluatePath(this.getCurrentHash());
				this.cachePath = record.getParent().resolve(record.getFileName() + ".data");
				Files.createDirectories(record.getParent());
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
		return this.realPath;
	}

	protected void initOldHash() throws IOException {
		if(this.initOldHash) {
			return;
		}
		this.initOldHash = true;

		this.getPath();
		Path path = this.cachePath;

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

	protected byte[] getCurrentHash() throws IOException {
		byte[] current = this.currentHash;
		if(current == null) {
			Hasher hasher = HASHING.newHasher();
			this.hashInputs(hasher);
			byte[] hash = hasher.hash().asBytes();
			if(hash.length != BYTES) {
				hash = Arrays.copyOf(hash, BYTES);
			}
			this.currentHash = hash;
			return hash;
		}
		return current;
	}

	protected abstract Iterable<Path> resolve0(Path resolvedPath, boolean isOutdated) throws IOException;

	public void writeHash() throws IOException {
		byte[] hash = this.getCurrentHash();
		this.getPath();
		try(OutputStream stream = Files.newOutputStream(this.cachePath)) {
			stream.write(hash, 0, BYTES);
			this.writeOutputs(stream);
		}
		this.oldHash = hash;
		this.isOutdated = 1;
	}

	@Override
	protected Iterable<Path> resolvePaths() throws IOException {
		boolean isOutdated = this.isOutdated();
		Path path = this.getPath();
		Iterable<Path> paths = this.resolve0(path, isOutdated);
		if(isOutdated) {
			this.writeHash();
		}
		return paths;
	}
}
