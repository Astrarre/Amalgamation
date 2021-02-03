/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.f2bb.amalgamation.gradle.impl.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;
import io.github.f2bb.amalgamation.gradle.base.BaseAmalgamationGradlePlugin;
import org.apache.tools.ant.filters.StringInputStream;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.internal.Pair;

public final class Cache {
	private final Logger logger;
	private final Project project;
	private final Path basePath;
	// todo global download path
	private final Path downloadPath;

	public Cache(Logger logger, Path basePath, Project project) throws IOException {
		this(logger, project, basePath, basePath.resolve("downloads"));
	}

	public Cache(Logger logger, Project project, Path basePath, Path downloadPath) throws IOException {
		this.logger = logger;
		this.project = project;
		this.basePath = basePath;
		this.downloadPath = downloadPath;
		Files.createDirectories(downloadPath);
		Files.createDirectories(basePath);
	}

	public static Cache of(Project project) throws IOException {
		return new Cache(project.getLogger(), project.getRootDir().toPath().resolve(".gradle").resolve("amalgamation").resolve("cache"), project);
	}

	public static Cache globalCache(Project project) {
		try {
			return new Cache(project.getLogger(), project.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation"),
	                project);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return the path and its hashcode
	 */
	public Pair<Path, String> computeIfAbsentHash(String output, UnsafeConsumer<PrimitiveSink> sink, UnsafeConsumer<Path> populator) {
		String hash = this.hash(sink);
		Path path = basePath.resolve(hash);
		Path out = path.resolve(output);
		Path ok = path.resolve("ok");

		// OK marker
		if (!Files.exists(ok)) {
			try {
				Files.createDirectories(path);
				populator.accept(out);
				Files.write(ok, new byte[0]);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}

		return Pair.of(out, hash);
	}

	/**
	 * @param output the output file name
	 * @param sink the sink to put the parts to hash
	 * @param populator write the contents to the path
	 */
	public Path computeIfAbsent(String output, UnsafeConsumer<PrimitiveSink> sink, UnsafeConsumer<Path> populator) {
		return this.computeIfAbsentHash(output, sink, populator).left;
	}

	public static String hash(UnsafeConsumer<PrimitiveSink> sink) {
		Hasher hasher = Hashing.sha256().newHasher();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		try (PrintStream printStream = new PrintStream(stream)) {
			try {
				sink.accept(new LoggingSink(hasher, printStream));
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}

		return hasher.hash().toString();
	}

	public Path download(String output, URL url, UnsafeConsumer<PrimitiveSink> sink) {
		Path path = this.downloadPath.resolve(this.hash(sink)).resolve(output);
		this.downloadIfChanged(url, path, this.logger, false);
		return path;
	}

	/**
	 * Download from the given {@link URL} to the given {@link Path} so long as there are differences between them.
	 *
	 * @param from The URL of the file to be downloaded
	 * @param to The destination to be saved to, and compared against if it exists
	 * @param logger The logger to print information to, typically from {@link Project#getLogger()}
	 * @param quiet Whether to only print warnings (when <code>true</code>) or everything
	 */
	private void downloadIfChanged(URL from, Path to, Logger logger, boolean quiet) {
		try {
			if (this.project.getGradle().getStartParameter().isOffline()) {
				logger.lifecycle("offline mode, skipping checks for " + from);
                if (Files.exists(to)) {
                    return;
                } else {
                	logger.lifecycle("unable to download url to " + to);
                    throw new IllegalStateException("no cached download for " + from);
                }
			}

			HttpURLConnection connection = (HttpURLConnection) from.openConnection();

			if (BaseAmalgamationGradlePlugin.refreshDependencies) {
				logger.lifecycle("deleting " + to);
				delete(to);
			}

			// If the output already exists we'll use it's last modified time
			if (Files.exists(to)) {
				connection.setIfModifiedSince(Files.getLastModifiedTime(to).toMillis());
			}

			//Try use the ETag if there's one for the file we're downloading
			String etag = loadETag(to, logger);

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
				throw new IOException(connection.getResponseMessage() + " for " + from);
			}

			long modifyTime = connection.getHeaderFieldDate("Last-Modified", -1);

			if (Files.exists(to) && (code == HttpURLConnection.HTTP_NOT_MODIFIED || modifyTime > 0 && Files.getLastModifiedTime(to)
			                                                                                               .toMillis() >= modifyTime)) {
				if (!quiet) {
					logger.lifecycle("'{}' Not Modified, skipping.", to);
				}

				return; //What we've got is already fine
			}

			long contentLength = connection.getContentLengthLong();

			if (!quiet && contentLength >= 0) {
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

			//Save the ETag (if we know it)
			String eTag = connection.getHeaderField("ETag");
			if (eTag != null) {
				//Log if we get a weak ETag and we're not on quiet
				if (!quiet && eTag.startsWith("W/")) {
					logger.warn("Weak ETag found.");
				}

				saveETag(to, eTag, logger);
			}
		} catch (Exception e) {
			try {
				delete(to);
			} catch (IOException ex) {
				throw new IllegalStateException("unable to delete " + to + " and/or it's etag file!");
			}
			throw new RuntimeException(e);
		}
	}


	// taken from Loom (MIT)
	// https://github.com/FabricMC/fabric-loom/blob/dev/0.6/src/main/java/net/fabricmc/loom/LoomGradlePlugin.java
	// FabricMC

	/**
	 * Delete the file along with the corresponding ETag, if it exists.
	 *
	 * @param file The file to delete.
	 */
	public void delete(Path file) throws IOException {
		if (Files.exists(file)) {
			Files.delete(file);
		}

		Path etagPath = getETagPath(file);
		if (Files.exists(etagPath)) {
			Files.delete(etagPath);
		}
	}

	/**
	 * Attempt to load an ETag for the given file, if it exists.
	 *
	 * @param to The file to load an ETag for
	 * @param logger The logger to print errors to if it goes wrong
	 * @return The ETag for the given file, or <code>null</code> if it doesn't exist
	 */
	private String loadETag(Path to, Logger logger) {
		Path eTagPath = getETagPath(to);

		if (!Files.exists(eTagPath)) {
			return null;
		}

		try {
			return String.join("\n", Files.readAllLines(eTagPath));
		} catch (IOException e) {
			logger.warn("Error reading ETag file '{}'.", eTagPath);
			return null;
		}
	}

	/**
	 * Format the given number of bytes as a more human readable string.
	 *
	 * @param bytes The number of bytes
	 * @return The given number of bytes formatted to kilobytes, megabytes or gigabytes if appropriate
	 */
	public String toNiceSize(long bytes) {
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

	/**
	 * Saves the given ETag for the given file, replacing it if it already exists.
	 *
	 * @param to The file to save the ETag for
	 * @param eTag The ETag to be saved
	 * @param logger The logger to print errors to if it goes wrong
	 */
	private void saveETag(Path to, String eTag, Logger logger) {
		Path eTagPath = getETagPath(to);

		try {
			Files.copy(new StringInputStream(eTag), eTagPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.warn("Error saving ETag file '{}'.", eTagPath, e);
		}
	}

	/**
	 * Creates a new file in the same directory as the given file with <code>.etag</code> on the end of the name.
	 *
	 * @param file The file to produce the ETag for
	 * @return The (uncreated) ETag file for the given file
	 */
	private Path getETagPath(Path file) {
		return file.getParent().resolve(file.getFileName() + ".etag");
	}

	// todo add proper url caching go steal from loom or smth
	public Path download(String output, URL url) {
		Path path = this.downloadPath.resolve(output);
		this.downloadIfChanged(url, path, this.logger, false);
		return path;
	}
}
