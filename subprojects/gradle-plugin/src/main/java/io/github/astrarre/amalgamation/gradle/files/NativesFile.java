package io.github.astrarre.amalgamation.gradle.files;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationImpl;
import io.github.astrarre.amalgamation.utils.CachedFile;
import io.github.astrarre.amalgamation.utils.Clock;
import io.github.astrarre.amalgamation.utils.DownloadUtil;
import io.github.astrarre.amalgamation.utils.LauncherMeta;
import org.jetbrains.annotations.Nullable;

public class NativesFile extends CachedFile<Set<String>> {
	private final MinecraftAmalgamationImpl amalgamation;
	private final String version;
	private final LauncherMeta meta;

	public NativesFile(MinecraftAmalgamationImpl amalgamation, String version, LauncherMeta meta) {
		super(BaseAmalgamationImpl.globalCache(amalgamation.project.getGradle()).resolve("natives").resolve(version), MinecraftAmalgamationImpl.SET);
		this.amalgamation = amalgamation;
		this.version = version;
		this.meta = meta;
	}

	@Override
	protected @Nullable Set<String> writeIfOutdated(Path path, @Nullable Set<String> currentData) throws Throwable {
		try (Clock clock = new Clock("Cache validation / download for natives-" + this.version + " took %sms", this.amalgamation.project.getLogger())) {
			if(BaseAmalgamationGradlePlugin.offlineMode) {
				return null;
			}
			if (currentData != null) {
				boolean allContained = true;
				for (LauncherMeta.Library library : this.meta.getVersion(this.version).getLibraries()) {
					for (LauncherMeta.HashedURL dependency : library.evaluateAllDependencies(LauncherMeta.NativesRule.NATIVES_ONLY)) {
						if (!currentData.contains(dependency.hash)) {
							allContained = false;
							break;
						}
					}
				}

				if (allContained) {
					return null;
				}
			}

			for (LauncherMeta.Library library : this.meta.getVersion(this.version).getLibraries()) {
				for (LauncherMeta.HashedURL dependency : library.evaluateAllDependencies(LauncherMeta.NativesRule.NATIVES_ONLY)) {
					DownloadUtil.Result result = DownloadUtil.read(dependency.getUrl(), null, -1, this.amalgamation.project.getLogger(),
							BaseAmalgamationGradlePlugin.offlineMode, false);
					if(result == null) {
						throw new IllegalStateException("unable to download natives!");
					}
					try(ZipInputStream input = new ZipInputStream(result.stream)) {
						ZipEntry entry;
						while ((entry = input.getNextEntry()) != null) {
							if(entry.isDirectory()) continue;
							Path toFile = path.resolve(entry.getName());
							if(Files.exists(toFile)) {
								this.amalgamation.project.getLogger().warn(toFile + " already exists!");
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
			return null;
		}
	}
}
