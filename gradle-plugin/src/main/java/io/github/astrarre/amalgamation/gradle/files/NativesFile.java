package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.jetbrains.annotations.Nullable;

public class NativesFile extends CachedFile { // todo use libraries directory, todo util to download thing by zip file
	private final MinecraftAmalgamationImpl amalgamation;
	private final String version;
	private final LauncherMeta meta;

	public NativesFile(MinecraftAmalgamationImpl amalgamation, String version, LauncherMeta meta) {
		super(AmalgIO.globalCache(amalgamation.project.getGradle()).resolve("natives").resolve(version));
		this.amalgamation = amalgamation;
		this.version = version;
		this.meta = meta;
	}

	@Override
	public void hashInputs(Hasher hasher) {
		hasher.putString(this.version, StandardCharsets.UTF_8);
	}

	@Override
	protected void write(Path output) throws IOException {
		LauncherMeta.Version vers = this.meta.getVersion(this.version);
		for(LauncherMeta.Library library : vers.getLibraries()) {
			for(LauncherMeta.HashedURL dependency : library.evaluateAllDependencies(LauncherMeta.NativesRule.NATIVES_ONLY)) {
				DownloadUtil.Result result = DownloadUtil.read(dependency.getUrl(),
						null,
						-1,
						this.amalgamation.project.getLogger(),
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
						Path toFile = output.resolve(entry.getName()); // todo apparently this should be flattened linux?
						if(Files.exists(toFile)) {
							if(!toFile.toString().contains("META-INF")) {
								this.amalgamation.project.getLogger().warn(toFile + " already exists!");
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
	}
}

