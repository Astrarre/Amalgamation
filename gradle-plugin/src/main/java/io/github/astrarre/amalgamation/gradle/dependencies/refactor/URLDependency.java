package io.github.astrarre.amalgamation.gradle.dependencies.refactor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import net.devtech.zipio.ZipTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

// todo this queries download url twice for some unknown reason...
public class URLDependency extends ZipProcessDependency {
	final String url;
	public boolean shouldOutput = true;
	public Path output;
	String etag;
	long lastModifyDate = -1;
	DownloadUtil.Result result;

	public URLDependency(Project project, String url) {
		super(project, "io.github.amalgamation", "download", "0.0.0");
		this.url = url;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		DownloadUtil.Result result = this.getResult();
		hasher.putString(this.url, StandardCharsets.UTF_8);
		if(result.etag != null) {
			hasher.putString(result.etag, StandardCharsets.UTF_8);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		if(this.output == null) {
			URL url = new URL(this.url);
			String host = url.getHost();
			return AmalgIO.cache(this.project, true).resolve("downloads").resolve(host).resolve(url.toString().replaceFirst(host, "$"));
		} else {
			return this.output;
		}
	}

	@Override
	protected void readInputs(@Nullable InputStream stream) throws IOException {
		if(stream != null) {
			DataInputStream data = new DataInputStream(stream);
			this.lastModifyDate = data.readLong();
			this.etag = data.readUTF();
			if(this.etag.isEmpty()) {
				this.etag = null;
			}
		}
	}

	@Override
	protected void writeOutputs(OutputStream stream) throws IOException {
		DataOutputStream data = new DataOutputStream(stream);
		if(this.result == null) {
			data.writeLong(this.lastModifyDate);
			data.writeUTF(this.etag == null ? "" : this.etag);
		} else {
			data.writeLong(result.lastModifyDate);
			data.writeUTF(result.etag);
		}
		data.flush();
	}

	public static Reader read(Project project, Dependency dependency) throws IOException {
		if(dependency instanceof URLDependency d) {
			return d.getOutdatedReader();
		} else {
			return Files.newBufferedReader(AmalgIO.resolve(project, dependency).toPath());
		}
	}

	public Reader getOutdatedReader() throws IOException {
		if(!Files.exists(this.getPath())) {
			this.resolve();
		}
		return Files.newBufferedReader(this.getPath());
	}

	@Nullable
	@Override
	public String getVersion() {
		try {
			return AmalgIO.b64(this.getCurrentHash());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void after(boolean isOutdated) {
		if(this.shouldOutput) {
			super.after(isOutdated);
		}
	}

	@Override
	protected Iterable<Path> resolve0(Path resolvedPath, boolean isOutdated) {
		if(isOutdated) {
			try(DownloadUtil.Result result = this.getResult()) {
				Files.copy(result.stream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
			} catch(Exception e) {
				throw new RuntimeException(url, e);
			}
		}
		return List.of(resolvedPath);
	}

	@Override
	protected void add(ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		Path path = this.shouldOutput ? resolvedPath : null;
		if(this.isOutdated()) {
			ZipTag tag = process.createZipTag(path);
			process.afterExecute(() -> {
				try(var result = this.getResult(); ZipInputStream zis = new ZipInputStream(result.stream)) {
					ReadableByteChannel channel = Channels.newChannel(zis);
					ZipEntry entry;
					while((entry = zis.getNextEntry()) != null) {
						ByteBuffer buffer = U.read(channel);
						tag.write(entry.getName(), buffer);
					}
				} catch(Exception e) {
					throw U.rethrow(e);
				}
			});
		} else if(path != null && Files.exists(path)) {
			process.addProcessed(path);
		} else {
			throw new IllegalStateException("shouldOutput is false, did not expect multi-use! " + this.url);
		}
	}

	protected DownloadUtil.Result getResult() throws IOException {
		if(this.result == null) {
			this.initOldHash();
			this.result = DownloadUtil.read(
					new URL(this.url),
					this.etag,
					this.lastModifyDate,
					this.getLogger(),
					BaseAmalgamationGradlePlugin.offlineMode,
					true);
		}
		return this.result;
	}

	/**
	 * I know, I know, shut up ir
	 */
	@Override
	protected void finalize() throws Throwable {
		if(this.result != null && this.result.stream != null) {
			this.result.stream.close();
		}
	}
}