package io.github.astrarre.amalgamation.gradle.dependencies;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class LibrariesDependency extends AbstractSelfResolvingDependency {
	/**
	 * defaults to your .minecraft installation, if not found, uses amalgamation cache
	 */
	public String librariesDirectory;
	/**
	 * states whether to include natives in libraries
	 */
	public LauncherMeta.NativesRule rule = LauncherMeta.NativesRule.ALL_NON_NATIVES;

	public LibrariesDependency(Project project, String version) {
		super(project, "net.minecraft", "minecraft-libraries", version);
		this.librariesDirectory = MinecraftAmalgamationGradlePlugin.getLibrariesCache(project);
	}

	@Override
	public Dependency copy() {
		return new LibrariesDependency(this.project, this.version);
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		return meta.getVersion(this.version)
				.getLibraries()
				.stream()
				.map(i -> i.evaluateAllDependencies(this.rule))
				.flatMap(Collection::stream)
				.map(dependency -> {
					Path jar = Paths.get(this.librariesDirectory).resolve(dependency.path);
					return CachedFile.forUrl(dependency, jar, this.project, false);
				})
				.map(CachedFile::getOutdatedPath)
				.collect(Collectors.toList());
	}
}
