package io.github.astrarre.amalgamation.gradle.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.function.Supplier;

import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class URLCachedFile<T> extends CachedFile<T> {
	private final URL url;
	private final Logger logger;
	private final boolean compress;

	public URLCachedFile(Path file, Type value, URL url, Logger logger, boolean compress) {
		super(file, value);
		this.url = url;
		this.logger = logger;
		this.compress = compress;
	}

	public URLCachedFile(Supplier<Path> file, Type value, URL url, Logger logger, boolean compress) {
		super(file, value);
		this.url = url;
		this.logger = logger;
		this.compress = compress;
	}

	@Nullable
	@Override
	protected T writeIfOutdated(Path to, @Nullable T data) throws Throwable {
		if(this.test(data)) {
			return null;
		}

		long lastModifyTime;
		if(Files.exists(to)) {
			lastModifyTime = Files.getLastModifiedTime(to).toMillis();
		} else {
			lastModifyTime = -1;
		}

		DownloadUtil.Result result = DownloadUtil.read(this.url, this.toEtag(data), lastModifyTime, this.logger, offlineMode, this.compress);
		if(result == null) return null;
		try(InputStream ignored = this.process(result.stream, to)) { // Try download to the output
			result.clock.close();
		} catch (IOException e) {
			CachedFile.delete(to);
			throw e;
		}

		//Set the modify time to match the server's (if we know it)
		if (result.lastModifyDate > 0) {
			Files.setLastModifiedTime(to, FileTime.fromMillis(result.lastModifyDate));
		}

		return this.newValue(result.etag);
	}

	protected InputStream process(InputStream input, Path file) throws IOException, URISyntaxException {
		Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
		return input;
	}

	protected boolean test(@Nullable T val) {
		return false;
	}

	@Nullable
	protected abstract String toEtag(T t);

	protected abstract T newValue(String etag);

	public static class Normal extends URLCachedFile<String> {
		public Normal(Path file, URL url, Logger logger, boolean compress) {
			super(file, String.class, url, logger, compress);
		}

		public Normal(Supplier<Path> file, URL url, Logger logger, boolean compress) {
			super(file, String.class, url, logger, compress);
		}

		@Override
		protected @Nullable String toEtag(String s) {
			return s;
		}

		@Override
		protected String newValue(String etag) {
			return etag;
		}
	}

	public static class Hashed extends URLCachedFile<String> {
		protected final String value;
		public Hashed(Path file, LauncherMeta.HashedURL url, Logger logger, boolean compress) {
			super(file, String.class, url.getUrl(), logger, compress);
			this.value = url.hash;
		}

		public Hashed(Supplier<Path> file, LauncherMeta.HashedURL url, Logger logger, boolean compress) {
			super(file, String.class, url.getUrl(), logger, compress);
			this.value = url.hash;
		}

		@Override
		protected boolean test(@Nullable String val) {
			return this.value.equals(val);
		}

		@Override
		protected @Nullable String toEtag(String s) {
			return null;
		}

		@Override
		protected String newValue(String etag) {
			return this.value;
		}
	}
}
