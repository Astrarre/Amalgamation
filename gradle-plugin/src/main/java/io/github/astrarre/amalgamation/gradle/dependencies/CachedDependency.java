package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
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
	byte isOutdated;
	private Path cachePath;

	public CachedDependency(Project project, String group, String name, String version) {
		super(project, group, name, version);
	}

	public boolean isOutdated() {
		byte val = this.isOutdated;
		if(val == 0) {
			try {
				this.initOldHash();
				if(this.currentHash == null) {
					this.computeHash();
				}

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

	public abstract void hashInputs(Hasher hasher);

	/**
	 * This path does not have to actually contain any files, filename.extension.data stores the information on this dependency
	 */
	protected abstract Path evaluatePath();

	protected void readInputs(@Nullable InputStream stream) throws IOException {}

	protected void writeOutputs(@Nullable OutputStream stream) throws IOException {}

	private void initOldHash() throws IOException {
		if(this.initOldHash) {
			return;
		}
		this.initOldHash = true;

		Path record = this.evaluatePath();
		Path path = this.cachePath = record.getParent().resolve(record.getFileName() + ".data");
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

	private byte[] computeHash() throws IOException {
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

	protected abstract Iterable<Path> resolve0(boolean isOutdated) throws IOException;

	private void writeHash() throws IOException {
		byte[] hash = this.computeHash();
		try(OutputStream stream = Files.newOutputStream(this.cachePath)) {
			stream.write(hash, 0, BYTES);
			this.writeOutputs(stream);
		}
	}

	@Override
	protected Iterable<Path> resolvePaths() throws IOException {
		boolean isOutdated = this.isOutdated();
		Iterable<Path> paths = this.resolve0(isOutdated);
		if(isOutdated) {
			this.writeHash();
		}
		return paths;
	}
}
