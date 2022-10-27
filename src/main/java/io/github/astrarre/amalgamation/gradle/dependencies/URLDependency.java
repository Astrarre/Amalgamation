package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.hash.Hasher;
import com.google.common.jimfs.Jimfs;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import java.nio.file.Path;
import java.nio.file.Path;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal;
import org.gradle.api.tasks.TaskDependency;
import org.jetbrains.annotations.Nullable;

// todo this queries download url twice for some unknown reason...
public class URLDependency extends CachedDependency implements SelfResolvingDependencyInternal {
	final String url;
	final boolean compressed;
	public String group, name, version;
	public boolean shouldOutput = true;
	public boolean isOptional = false;
	public boolean silent = false;
	public boolean isUnique = false;
	public Path output;
	String etag;
	long lastModifyDate = -1;
	DownloadUtil.Result result;

	public URLDependency(Project project, String url) {
		this(project, url, true);
	}

	public URLDependency(Project project, String url, boolean compressed) {
		super(project);
		this.url = url;
		this.compressed = compressed;
		try {
			URL ur = new URL(url);
			this.group = ur.getHost();
			this.name = strip(ur.getPath());
			this.version = "NaN";
		} catch(MalformedURLException e) {
			throw Err.rethrow(e);
		}
	}

	@Override
	public Path getPath() {
		return this.output != null ? this.output : super.getPath();
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putString(this.url, StandardCharsets.UTF_8);
		if(!isUnique) {
			DownloadUtil.Result result = this.getResult();
			if(result != null && result.etag != null) {
				hasher.putString(result.etag, StandardCharsets.UTF_8);
			}
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		if(this.output == null) {
			URL url = new URL(this.url);
			String host = url.getHost();
			return AmalgDirs.GLOBAL.downloads(this.project).resolve(host+"/"+url.toString().replaceFirst(host, "$"));
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

	public BufferedReader getOutdatedReader() throws IOException {
		if(!Files.exists(this.getCachePath())) {
			this.getArtifacts();
		}
		return Files.newBufferedReader(this.getPath());
	}

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			DownloadUtil.Result result = this.getResult();
			if(result.error != null) {
				if(this.isOptional) {
					return Set.of();
				} else {
					throw result.error;
				}
			}

			try(result) {
				// todo in memory file system in the cache, prolly with Jimfs
				AmalgIO.createParent(resolvedPath);
				Files.copy(result.stream, resolvedPath);
			}
		}

		return Set.of(createArtifact(resolvedPath));
	}

	static String strip(String s) {
		int start = s.lastIndexOf('/')+1, end = s.lastIndexOf('.');
		return end == -1 ? s.substring(start) : s.substring(start, end);
	}

	Artifact createArtifact(Path path) throws MalformedURLException {
		return new Artifact.File(
				this.project, this,
				path,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
	}

	protected DownloadUtil.Result getResult() throws IOException {
		if(this.result == null) {
			this.initOldHash();
			this.result = DownloadUtil.read(
					new URL(this.url),
					this.etag,
					this.lastModifyDate,
					this.silent ? null : this.logger,
					BaseAmalgamationGradlePlugin.offlineMode,
					this.compressed);
			if(!this.isOptional && this.result != null && this.result.error != null) {
				throw this.result.error;
			}
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

	@Nullable
	@Override
	public ComponentIdentifier getTargetComponentId() {
		return this::toString;
	}

	@Override
	public Set<File> resolve() {
		return this.getArtifacts()
				       .stream()
				       .map(a -> a.file)
				       .map(Path::toFile)
				       .collect(Collectors.toSet());
	}

	@Override
	public Set<File> resolve(boolean transitive) {
		return resolve();
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return task -> Set.of();
	}

	@Nullable
	@Override
	public String getGroup() {
		return this.group;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Nullable
	@Override
	public String getVersion() {
		return this.version;
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		return this.equals(dependency);
	}

	@Override
	public Dependency copy() {
		return this;
	}

	String reason;
	@Nullable
	@Override
	public String getReason() {
		return this.reason;
	}

	@Override
	public void because(@Nullable String reason) {
		this.reason = reason;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(!(o instanceof URLDependency objects)) {
			return false;
		}
		if(!super.equals(o)) {
			return false;
		}

		if(this.compressed != objects.compressed) {
			return false;
		}
		if(this.shouldOutput != objects.shouldOutput) {
			return false;
		}
		if(this.isOptional != objects.isOptional) {
			return false;
		}
		if(!Objects.equals(this.url, objects.url)) {
			return false;
		}
		if(!Objects.equals(this.output, objects.output)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result1 = super.hashCode();
		result1 = 31 * result1 + (this.url != null ? this.url.hashCode() : 0);
		result1 = 31 * result1 + (this.compressed ? 1 : 0);
		result1 = 31 * result1 + (this.shouldOutput ? 1 : 0);
		result1 = 31 * result1 + (this.isOptional ? 1 : 0);
		result1 = 31 * result1 + (this.output != null ? this.output.hashCode() : 0);
		return result1;
	}
}
