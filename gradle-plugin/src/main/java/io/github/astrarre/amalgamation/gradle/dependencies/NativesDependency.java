package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import org.gradle.api.Project;

public class NativesDependency extends CachedDependency {
	final String version;
	final List<LauncherMeta.HashedURL> dependencies;
	final Path nativesDir;

	public NativesDependency(Project project, String version) {
		super(project);
		this.version = version;
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		LauncherMeta.Version vers = meta.getVersion(version);
		this.dependencies = new ArrayList<>();
		for(LauncherMeta.Library library : vers.getLibraries()) {
			this.dependencies.addAll(library.evaluateAllDependencies(LauncherMeta.NativesRule.NATIVES_ONLY));
		}
		this.nativesDir = AmalgIO.cache(project, true).resolve(version).resolve("natives");
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		for(LauncherMeta.HashedURL dependency : this.dependencies) {
			hasher.putString(dependency.hash, StandardCharsets.UTF_8);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return this.nativesDir;
	}

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			for(Path path : UnsafeIterable.walkFiles(resolvedPath)) {
				Files.delete(path);
			}
			// natives take very little time and rarely change, but it may be worth considering pulling natives from older versions?
			for(LauncherMeta.HashedURL dependency : this.dependencies) {
				DownloadUtil.Result result = DownloadUtil.read(dependency.getUrl(),
						null,
						-1,
						this.logger,
						BaseAmalgamationGradlePlugin.offlineMode,
						false);
				if(result == null) {
					throw new IllegalStateException("unable to download natives!");
				}
				try(ZipInputStream input = new ZipInputStream(result.stream)) {
					ZipEntry entry;
					while((entry = input.getNextEntry()) != null) {
						if(entry.isDirectory()) {
							continue;
						}
						Path toFile = resolvedPath.resolve(entry.getName()); // todo apparently this should be flattened linux?
						if(Files.exists(toFile)) {
							if(!toFile.toString().contains("META-INF")) {
								this.logger.warn(toFile + " already exists!");
							}
							continue;
						}
						Path parent = toFile.getParent();
						if(parent != null && !Files.exists(parent)) {
							Files.createDirectories(parent);
						}
						Files.copy(input, toFile);
						input.closeEntry();
					}
				}
			}
		}
		return Set.of(new Artifact.File(
				this.project,
				"net.minecraft", "natives", version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		));
	}
}
