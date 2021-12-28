package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class HashedURLDependency extends URLDependency {
	public final String hash;
	public HashedURLDependency(Project project, LauncherMeta.HashedURL url) {
		super(project, url.url);
		this.hash = url.hash;
	}

	public HashedURLDependency(Project project, LauncherMeta.HashedURL url, boolean compressed) {
		super(project, url.url, compressed);
		this.hash = url.hash;
	}

	@Override
	protected void readInputs(@Nullable InputStream stream) throws IOException {
	}

	@Override
	protected void writeOutputs(OutputStream stream) throws IOException {
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putString(this.hash, StandardCharsets.UTF_8);
	}
}
