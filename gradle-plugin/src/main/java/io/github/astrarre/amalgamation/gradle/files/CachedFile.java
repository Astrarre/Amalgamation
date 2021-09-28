package io.github.astrarre.amalgamation.gradle.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.Lazy;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public abstract class CachedFile {
	public static final Gson GSON = new Gson();
	public static boolean refreshAmalgamationCaches, offlineMode;
	final Lazy<Path> output;
	final Lazy<Path> data;

	/**
	 * @param file the location of the file
	 */
	public CachedFile(Path file) {
		this(() -> file);
		try {
			Files.createDirectories(file.getParent());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CachedFile(Supplier<Path> file) {
		this.output = Lazy.of(file);
		this.data = this.output.map(p -> p.getParent().resolve(p.getFileName() + ".data"));
	}

	public static CachedFile forUrl(String url, Path path, Logger logger, boolean compress) {
		try {
			return forUrl(new URL(url), path, logger, compress);
		} catch(MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static CachedFile forUrl(LauncherMeta.HashedURL url, Path path, Logger logger, boolean compress) {
		return new URLCachedFile.Hashed(path, url, logger, compress);
	}

	public static CachedFile forUrl(URL url, Path path, Logger logger, boolean compress) {
		return new URLCachedFile.Normal(path, url, logger, compress);
	}

	/**
	 * Format the given number of bytes as a more human readable string.
	 *
	 * @param bytes The number of bytes
	 * @return The given number of bytes formatted to kilobytes, megabytes or gigabytes if appropriate
	 */
	public static String toNiceSize(long bytes) {
		if(bytes < 1024) {
			return bytes + " B";
		} else if(bytes < 1024 * 1024) {
			return bytes / 1024 + " KB";
		} else if(bytes < 1024 * 1024 * 1024) {
			return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
		} else {
			return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
		}
	}

	public abstract void hashInputs(Hasher hasher);

	protected abstract void write(Path output) throws IOException;

	public boolean isOutdated() {
		Hasher hasher = Hashing.sha256().newHasher();
		this.hashInputs(hasher);
		HashCode code = hasher.hash();
		HashCode current = this.currentHash();
		return current == null || current.equals(code);
	}

	protected void updateHash() {
		Hasher hasher = Hashing.sha256().newHasher();
		this.hashInputs(hasher);
		HashCode code = hasher.hash();
		this.setHash(code);
	}

	public Path getOutdatedPath() {
		try {
			Path path = this.path();
			if(refreshAmalgamationCaches) {
				Files.deleteIfExists(path);
			}

			if(Files.exists(path)) {
				return path;
			}

			return this.getOutput();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Path getOutput() {
		try {
			boolean refresh = false;
			Path path = this.path();
			if(refreshAmalgamationCaches) {
				Files.deleteIfExists(this.data());
				if(Files.isDirectory(path)) {
					delete(path);
				} else {
					Files.deleteIfExists(path);
				}
				refresh = true;
			}

			return this.getPath(refresh);
		} catch(Exception e) {
			throw U.rethrow(e);
		}
	}

	protected Path getPath(boolean refresh) throws IOException {
		Hasher hasher = Hashing.sha256().newHasher();
		this.hashInputs(hasher);
		HashCode code = hasher.hash();
		if(!refresh) {
			HashCode old = this.currentHash();
			refresh = old == null || !old.equals(code);
		}
		if(refresh) {
			this.write(this.path());
			this.setHash(code);
		}
		return this.path();
	}

	@Nullable
	public HashCode currentHash() {
		Path data = this.data();
		if(Files.exists(data)) {
			try {
				return HashCode.fromBytes(Files.readAllBytes(data));
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		} else {
			return null;
		}
	}

	protected void setHash(HashCode code) {
		try {
			Files.write(this.data(), code.asBytes());
		} catch(IOException e) {
			throw U.rethrow(e);
		}
	}

	public Path path() {
		return this.output.get();
	}

	protected Path data() {
		return this.data.get();
	}

	public static void delete(Path path) throws IOException {
		if(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try(DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for(Path entry : entries) {
					delete(entry);
				}
			}
		}
		Files.deleteIfExists(path);
	}

	/**
	 * if the file exists, it will return the reader, else it will update the file and get the reader. refresh dependencies overrides this behavior
	 *
	 * @return a reader for the file that may not be up to date
	 */
	public Reader getOutdatedReader() {
		try {
			Path path = this.path();
			if(!Files.exists(path) || refreshAmalgamationCaches) {
				this.getOutput();
			}
			return Files.newBufferedReader(path);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Reader getReader() {
		try {
			return Files.newBufferedReader(this.getOutput());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream getInputStream() {
		try {
			return Files.newInputStream(this.getOutput());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}


	public void delete() {
		try {
			delete(this.data());
			delete(this.path());
		} catch(IOException e) {
			throw U.rethrow(e);
		}
	}

	public static void deleteCached(Path path) throws IOException {
		delete(path.getParent().resolve(path.getFileName() + ".data"));
		delete(path);
	}
}
