package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import net.devtech.zipio.ZipTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class URLCachedFile extends CommonStateCachedFile {
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

	protected void hash(Hasher hasher, String etag) {
		hasher.putString(etag, StandardCharsets.UTF_8);
	}

	@Override
	protected void hashAndWrite(Hasher hasher, Path to) throws Throwable {
		long lastModifyTime;
		if(Files.exists(to)) {
			lastModifyTime = Files.getLastModifiedTime(to).toMillis();
		} else {
			lastModifyTime = -1;
		}

		this.preConnect(hasher);

		DownloadUtil.Result result = this.getResult(this.etag(to), lastModifyTime);
		if(result == null) {
			return;
		}

		this.postConnect(hasher, result);

		try(InputStream ignored = this.process(result.stream, to)) { // Try download to the output
			result.clock.close();
		}
		if(result.lastModifyDate > 0) {
			Files.setLastModifiedTime(to, FileTime.fromMillis(result.lastModifyDate));
		}
		Path out = to.getParent().resolve(to.getFileName().toString() + ".etag");
		Files.createDirectories(out.getParent());
		Files.writeString(out, result.etag);
	}

	private void postConnect(Hasher hasher, DownloadUtil.Result result) {
		this.hash(hasher, result.etag);
		this.tryTerminate(() -> {
			result.clock.close();
			UCons.run(p -> result.stream.close());
		});
	}

	void preConnect(Hasher hasher) {

	}

	@Nullable
	protected DownloadUtil.Result getResult(String etag, long lastModifyTime) throws IOException {
		DownloadUtil.Result result = DownloadUtil.read(this.url, etag, lastModifyTime, this.logger, offlineMode, this.compress);
		if(result == null || result.stream == null) {
			return null;
		}
		return result;
	}

	protected String etag(Path to) throws IOException {
		Path out = to.getParent().resolve(to.getFileName().toString() + ".etag");
		return Files.exists(out) ? Files.readString(out) : null;
	}

	protected InputStream process(InputStream input, Path file) throws IOException, URISyntaxException {
		Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
		return input;
	}

	public static class Hashed extends ZipProcessCachedFile {
		protected final LauncherMeta.HashedURL value;
		public final boolean compress;
		public boolean shouldOutput = true;

		public Hashed(Path file, Project project, LauncherMeta.HashedURL value, boolean compress) {
			super(file, project);
			this.value = value;
			this.compress = compress;
		}

		@Override
		public void hashInputs(Hasher hasher) {
			hasher.putString(this.value.hash, StandardCharsets.UTF_8);
		}

		@Override
		public void init(ZipProcessBuilder builder, Path outputFile) {
			Path h = this.path();
			Path path = this.shouldOutput ? h : null;
			if(this.isOutdated()) {
				ZipTag tag = builder.createZipTag(path);
				builder.afterExecute(() -> {
					try(ZipInputStream zis = new ZipInputStream(this.getRead().stream)) {
						ReadableByteChannel channel = Channels.newChannel(zis);
						ZipEntry entry;
						while((entry = zis.getNextEntry()) != null) {
							ByteBuffer buffer = U.read(channel);
							tag.write(entry.getName(), buffer);
						}
					} catch(IOException e) {
						throw U.rethrow(e);
					}
				});
			} else if(this.shouldOutput) {
				builder.addProcessed(path);
			}
		}

		@Override
		protected void write(Path output) throws IOException {
			DownloadUtil.Result result = this.needsUrl() ? this.getRead() : null;
			if(result == null || result.stream == null) {
				throw new IllegalStateException("Unable to download " + this.value.url);
			} else {
				this.write(output, result);
			}
		}

		protected DownloadUtil.Result getRead() throws IOException {
			return DownloadUtil.read(this.value.getUrl(), null, -1, this.project.getLogger(), offlineMode, this.compress);
		}

		protected void write(Path output, DownloadUtil.Result result) throws IOException {
			Files.copy(result.stream, output, StandardCopyOption.REPLACE_EXISTING);
		}

		protected boolean needsUrl() {
			return true;
		}
	}
}
