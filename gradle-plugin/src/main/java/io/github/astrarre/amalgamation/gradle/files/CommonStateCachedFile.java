package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * For file where the state nessesary for hashing is also the state nessesary for writing, for example
 *  hashing a download requires connecting to the server, but so does actually downloading the file.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class CommonStateCachedFile extends CachedFile {
	static final class Terminator extends Error {
	}

	Lock lock = new ReentrantLock();
	Hasher hashing;
	HashCode set;
	boolean test;
	Runnable action;

	public CommonStateCachedFile(Supplier<Path> output) {
		super(output);
	}

	public CommonStateCachedFile(Path file) {
		super(file);
	}

	/**
	 * Hash the inputs, then call {@link #tryTerminate(Runnable)}
	 */
	protected abstract void hashAndWrite(Hasher hasher, Path output) throws Throwable;

	protected void tryTerminate() {
		this.tryTerminate(() -> {});
	}

	protected void tryTerminate(Runnable after) {
		this.action = after;
		if(this.test) {
			throw new Terminator();
		} else {
			HashCode code = this.currentHash();
			HashCode hash = this.hashing.hash();
			if(code != null && code.equals(hash)) {
				throw new Terminator();
			} else {
				this.set = hash;
			}
		}
	}

	@Override
	protected Path getPath(boolean refresh) throws IOException {
		this.write(this.path());
		return this.path();
	}

	@Override
	public void hashInputs(Hasher hasher) {
		this.lock.lock();
		this.test = true;
		try {
			this.hashAndWrite(hasher, this.path());
		} catch(Terminator ignored) {
		} catch(Throwable throwable) {
			throw new RuntimeException(throwable);
		} finally {
			this.test = false;
			if(this.action != null) {
				this.action.run();
				this.action = null;
			}
			this.lock.unlock();
		}
	}

	@Override
	protected void write(Path output) {
		this.lock.lock();
		Hasher hasher = Hashing.sha256().newHasher();
		boolean terminated = false;
		try {
			this.hashing = hasher;
			this.hashAndWrite(hasher, output);
		} catch(Terminator ignored) {
			// hash was equal
			terminated = true;
		} catch(Throwable throwable) {
			throw new RuntimeException(throwable);
		} finally {
			if(!terminated && this.set != null) {
				this.setHash(this.set);
				this.set = null;
			}
			if(this.action != null) {
				this.action.run();
				this.action = null;
			}
			this.lock.unlock();
		}
	}
}
