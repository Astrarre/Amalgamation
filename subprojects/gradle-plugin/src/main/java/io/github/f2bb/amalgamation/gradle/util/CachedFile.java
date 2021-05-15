package io.github.f2bb.amalgamation.gradle.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.util.func.UnsafeConsumer;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class CachedFile<T> {
	private static final Gson GSON = BaseAmalgamationGradlePlugin.GSON;
	private final Supplier<Path> file;
	private final Lock lock = new ReentrantLock();
	private final Class<T> value;

	/**
	 * @param file the location of the file
	 */
	public CachedFile(Path file, Class<T> value) {
		this(() -> file, value);
		try {
			Files.createDirectories(file.getParent());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CachedFile(Supplier<Path> file, Class<T> value) {
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

	public static Path globalCache(Gradle gradle) {
		return gradle.getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation");
	}

	public static Path projectCache(Project project) {
		return project.getBuildDir().toPath().resolve("amalgamation-caches");
	}

	public static Path forHash(Path dir, UnsafeConsumer<PrimitiveSink> hashing) {
		Hasher hasher = Hashing.sha256().newHasher();
		hashing.acceptFailException(hasher);
		return dir.resolve(hasher.hash().toString());
	}

	public static CachedFile<String> forUrl(String url, Path path, Logger logger) {
		try {
			return forUrl(new URL(url), path, logger);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static CachedFile<String> forUrl(LauncherMeta.HashedURL url, Path path, Logger logger) {
		return new CachedFile<String>(path, String.class) {
			@Nullable
			@Override
			protected String writeIfOutdated(Path to, @Nullable String hash) throws IOException {
				if(url.hash.equals(hash)) {
					return null;
				}
				Clock clock = new Clock("Validating/Downloading " + url + " cache took %dms", logger);
				HttpURLConnection connection = (HttpURLConnection) url.getUrl().openConnection();
				// If the output already exists we'll use it's last modified time
				if (Files.exists(to)) {
					if (BaseAmalgamationGradlePlugin.offlineMode) {
						clock.close();
						return null;
					}

					connection.setIfModifiedSince(Files.getLastModifiedTime(to).toMillis());
				}

				// We want to download gzip compressed stuff
				connection.setRequestProperty("Accept-Encoding", "gzip");

				// Try make the connection, it will hang here if the connection is bad
				connection.connect();

				int code = connection.getResponseCode();

				if ((code < 200 || code > 299) && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
					//Didn't get what we expected
					clock.close();
					throw new IOException(connection.getResponseMessage() + " for " + url);
				}

				long modifyTime = connection.getHeaderFieldDate("Last-Modified", -1);

				if (Files.exists(to) && (code == HttpURLConnection.HTTP_NOT_MODIFIED || modifyTime > 0 && Files.getLastModifiedTime(to)
				                                                                                               .toMillis() >= modifyTime)) {
					logger.lifecycle("'{}' Not Modified, skipping.", to);
					clock.close();
					return null; //What we've got is already fine
				}

				long contentLength = connection.getContentLengthLong();

				if (contentLength >= 0) {
					logger.lifecycle("'{}' Changed, downloading {}", to, toNiceSize(contentLength));
				}

				try { // Try download to the output
					InputStream stream = connection.getInputStream();
					Files.copy(stream, to, StandardCopyOption.REPLACE_EXISTING);
					stream.close();
				} catch (IOException e) {
					Files.delete(to); // Probably isn't good if it fails to copy/save
					clock.close();
					throw e;
				}

				//Set the modify time to match the server's (if we know it)
				if (modifyTime > 0) {
					Files.setLastModifiedTime(to, FileTime.fromMillis(modifyTime));
				}

				clock.close();
				return url.hash;
			}
		};
	}

	public static CachedFile<String> forUrl(URL url, Path path, Logger logger) {
		return new CachedFile<String>(path, String.class) {
			@Nullable
			@Override
			protected String writeIfOutdated(Path to, @Nullable String etag) throws IOException {
				HttpURLConnection connection;

				try (Clock ignored = new Clock("Validating " + url + " cache took %dms", logger)) {
					connection = (HttpURLConnection) url.openConnection();
					// If the output already exists we'll use it's last modified time
					if (Files.exists(to)) {
						if (BaseAmalgamationGradlePlugin.offlineMode) {
							return etag;
						}

						connection.setIfModifiedSince(Files.getLastModifiedTime(to).toMillis());
					}

					if (etag != null) {
						connection.setRequestProperty("If-None-Match", etag);
					}

					// We want to download gzip compressed stuff
					connection.setRequestProperty("Accept-Encoding", "gzip");

					// Try make the connection, it will hang here if the connection is bad
					connection.connect();

					int code = connection.getResponseCode();

					if ((code < 200 || code > 299) && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
						//Didn't get what we expected
						throw new IOException(connection.getResponseMessage() + " for " + url);
					}

					long modifyTime = connection.getHeaderFieldDate("Last-Modified", -1);

					if (Files.exists(to) && (code == HttpURLConnection.HTTP_NOT_MODIFIED || modifyTime > 0 && Files.getLastModifiedTime(to)
							.toMillis() >= modifyTime)) {
						logger.lifecycle("'{}' Not Modified, skipping.", to);
						return null; //What we've got is already fine
					}

					long contentLength = connection.getContentLengthLong();

					if (contentLength >= 0) {
						logger.lifecycle("'{}' Changed, downloading {}", to, toNiceSize(contentLength));
					}

					try { // Try download to the output
						Files.copy(connection.getInputStream(), to, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						Files.delete(to); // Probably isn't good if it fails to copy/save
						throw e;
					}

					//Set the modify time to match the server's (if we know it)
					if (modifyTime > 0) {
						Files.setLastModifiedTime(to, FileTime.fromMillis(modifyTime));
					}
				}

				return connection.getHeaderField("ETag");
			}
		};
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
			if (BaseAmalgamationGradlePlugin.refreshAmalgamationCaches) {
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
			if (BaseAmalgamationGradlePlugin.refreshAmalgamationCaches) {
				Files.deleteIfExists(this.file.get());
			}

			this.lock.lock();
			T old = this.getData();
			T data = this.writeIfOutdated(this.file.get(), old);
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


	/**
	 * if the file exists, it will return the reader, else it will update the file and get the reader.
	 * refresh dependencies overrides this behavior
	 */
	public Reader getOutdatedReader() {
		try {
			Path path = this.file.get();
			if (!Files.exists(path) || BaseAmalgamationGradlePlugin.refreshAmalgamationCaches) {
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
