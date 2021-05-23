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

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.google.gson.Gson;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeConsumer;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class CachedFile<T> {
	public static boolean refreshAmalgamationCaches, offlineMode;
	public static final Gson GSON = new Gson();
	private final Supplier<Path> file;
	private final Lock lock = new ReentrantLock();
	private final Type value;

	/**
	 * @param file the location of the file
	 * @param value the type so gson can deserialize the data
	 */
	public CachedFile(Path file, Type value) {
		this(() -> file, value);
		try {
			Files.createDirectories(file.getParent());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CachedFile(Supplier<Path> file, Type value) {
		this.file = new Supplier<Path>() {
			Path lazy;

			@Override
			public Path get() {
				Path lazy = this.lazy;
				if (lazy == null) {
					lazy = this.lazy = file.get();
				}
				return lazy;
			}
		};
		this.value = value;
	}

	public static Path forHash(Path dir, UnsafeConsumer<PrimitiveSink> hashing) {
		Hasher hasher = Hashing.sha256().newHasher();
		hashing.acceptFailException(hasher);
		return dir.resolve(hasher.hash().toString());
	}

	public static CachedFile<String> forUrl(String url, Path path, Logger logger, boolean compress) {
		try {
			return forUrl(new URL(url), path, logger, compress);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static CachedFile<String> forUrl(LauncherMeta.HashedURL url, Path path, Logger logger, boolean compress) {
		return new URLCachedFile.Hashed(path, url, logger, compress);
	}

	public static CachedFile<String> forUrl(URL url, Path path, Logger logger, boolean compress) {
		return new URLCachedFile.Normal(path, url, logger, compress);
	}

	/**
	 * Format the given number of bytes as a more human readable string.
	 *
	 * @param bytes The number of bytes
	 * @return The given number of bytes formatted to kilobytes, megabytes or gigabytes if appropriate
	 */
	public static String toNiceSize(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		} else if (bytes < 1024 * 1024) {
			return bytes / 1024 + " KB";
		} else if (bytes < 1024 * 1024 * 1024) {
			return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
		} else {
			return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
		}
	}

	public void delete() throws IOException {
		Files.deleteIfExists(this.file.get().getParent().resolve(this.file.get().getFileName() + ".data"));
		Files.deleteIfExists(this.file.get());
	}

	public Reader getReader() {
		try {
			this.getPath();
			return Files.newBufferedReader(this.file.get());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Path getOutdatedPath() {
		try {
			Path path = this.file.get();
			if (refreshAmalgamationCaches) {
				Files.deleteIfExists(path);
			}

			if (Files.exists(path)) {
				return path;
			}

			return this.getPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Path getPath() {
		try {
			Path path = this.file.get();
			if (refreshAmalgamationCaches) {
				Files.deleteIfExists(path.toAbsolutePath().getParent().resolve(path.getFileName() + ".data"));
				if(Files.isDirectory(path)) {
					delete(path);
				} else {
					Files.deleteIfExists(path);
				}
			}

			this.lock.lock();
			T old = this.getData();
			T data = this.writeIfOutdated(path, old);
			if (data != null) {
				this.setData(data);
			}
			this.lock.unlock();
			return this.file.get();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	@Nullable
	protected abstract T writeIfOutdated(Path path, @Nullable T currentData) throws Throwable;

	public T getData() {
		try {
			Path path = this.file.get().getParent().resolve(this.file.get().getFileName() + ".data");
			if (Files.exists(path)) {
				try(BufferedReader reader = Files.newBufferedReader(path)) {
					return GSON.fromJson(reader, this.value);
				}
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setData(T data) {
		try {
			Path path = this.file.get().getParent().resolve(this.file.get().getFileName() + ".data");
			try(BufferedWriter writer = Files.newBufferedWriter(path)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void delete(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					delete(entry);
				}
			}
		}
		Files.deleteIfExists(path);
	}


	/**
	 * if the file exists, it will return the reader, else it will update the file and get the reader.
	 * refresh dependencies overrides this behavior
	 * @return a reader for the file that may not be up to date
	 */
	public Reader getOutdatedReader() {
		try {
			Path path = this.file.get();
			if (!Files.exists(path) || refreshAmalgamationCaches) {
				this.getPath();
			}
			return Files.newBufferedReader(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream getInputStream() {
		try {
			this.getPath();
			return Files.newInputStream(this.file.get());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
