package io.github.astrarre.amalgamation.gradle.dependencies;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamation;
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
	protected Iterable<Path> resolvePaths() {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		return Iterables.concat(Iterables.transform(
				Objects.requireNonNull(meta.getVersion(this.version), "Invalid version: " + this.version)
				       .getLibraries(),
				input -> Iterables.transform(input.evaluateAllDependencies(this.rule), dependency -> {
					Path jar = Paths.get(this.librariesDirectory).resolve(dependency.path);
					return CachedFile.forUrl(dependency, jar, this.project.getLogger(), false).getOutdatedPath();
				})));
	}

	@Override
	public Dependency copy() {
		return new LibrariesDependency(this.project, this.version);
	}
}
