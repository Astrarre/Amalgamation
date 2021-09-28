package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.function.Supplier;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import org.gradle.api.logging.Logger;

public abstract class URLCachedFile extends CommonStateCachedFile {
	private final URL url;
	private final Logger logger;
	private final boolean compress;

	public URLCachedFile(Supplier<Path> output, URL url, Logger logger, boolean compress) {
		super(output);
		this.url = url;
		this.logger = logger;
		this.compress = compress;
	}

	public URLCachedFile(Path file, URL url, Logger logger, boolean compress) {
		super(file);
		this.url = url;
		this.logger = logger;
		this.compress = compress;
	}

	@Override
	protected void hashAndWrite(Hasher hasher, Path to) throws Throwable {
		long lastModifyTime;
		if(Files.exists(to)) {
			lastModifyTime = Files.getLastModifiedTime(to).toMillis();
		} else {
			lastModifyTime = -1;
		}

		DownloadUtil.Result result = DownloadUtil.read(this.url, this.etag(to), lastModifyTime, this.logger, offlineMode, this.compress);
		if(result == null || result.stream == null) {
			return;
		}

		this.hash(hasher, result.etag);
		this.tryTerminate(() -> {
			result.clock.close();
			UCons.run(p -> result.stream.close());
		});

		try(InputStream ignored = this.process(result.stream, to)) { // Try download to the output
			result.clock.close();
		}
		if(result.lastModifyDate > 0) {
			Files.setLastModifiedTime(to, FileTime.fromMillis(result.lastModifyDate));
		}
		this.saveEtag(to, result.etag);
	}

	protected void saveEtag(Path to, String etag) throws IOException {
		Path out = to.getParent().resolve(to.getFileName().toString() + ".etag");
		Files.createDirectories(out.getParent());
		Files.writeString(out, etag);
	}

	protected String etag(Path to) throws IOException {
		Path out = to.getParent().resolve(to.getFileName().toString() + ".etag");
		return Files.exists(out) ? Files.readString(out) : null;
	}

	protected abstract void hash(Hasher hasher, String etag);

	protected InputStream process(InputStream input, Path file) throws IOException, URISyntaxException {
		Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
		return input;
	}

	public static class Normal extends URLCachedFile {
		public Normal(Supplier<Path> output, URL url, Logger logger, boolean compress) {
			super(output, url, logger, compress);
		}

		public Normal(Path file, URL url, Logger logger, boolean compress) {
			super(file, url, logger, compress);
		}

		@Override
		protected void hash(Hasher hasher, String etag) {
			hasher.putString(etag, StandardCharsets.UTF_8);
		}
	}

	public static class Hashed extends URLCachedFile {
		protected final String value;

		public Hashed(Path file, LauncherMeta.HashedURL url, Logger logger, boolean compress) {
			super(file, url.getUrl(), logger, compress);
			this.value = url.hash;
		}

		public Hashed(Supplier<Path> file, LauncherMeta.HashedURL url, Logger logger, boolean compress) {
			super(file, url.getUrl(), logger, compress);
			this.value = url.hash;
		}

		@Override
		protected void hash(Hasher hasher, String etag) {
			hasher.putString(this.value, StandardCharsets.UTF_8);
		}

		@Override
		public void hashInputs(Hasher hasher) {
			hasher.putString(this.value, StandardCharsets.UTF_8);
		}
	}
}
