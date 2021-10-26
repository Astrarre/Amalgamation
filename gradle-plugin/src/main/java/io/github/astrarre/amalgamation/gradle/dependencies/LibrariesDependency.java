package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.dsl.DependencyHandler;

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
	protected Iterable<Path> resolvePaths() throws IOException {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		final Path dir = Paths.get(this.librariesDirectory);
		List<LauncherMeta.Library> libraries = meta.getVersion(this.version).getLibraries();
		List<File> files = new ArrayList<>();

		for(LauncherMeta.Library library : libraries) {
			boolean failedDirectDownload = false; // use maven as fallback incase using the URL does not work (maybe mojang servers down?)
			for(LauncherMeta.HashedURL dependency : library.evaluateAllDependencies(this.rule)) {
				Path jar = dir.resolve(dependency.path);
				HashedURLDependency dep = new HashedURLDependency(this.project, dependency);
				dep.output = jar;
				dep.isOptional = true;
				var resolved = dep.resolvePaths();
				if(!resolved.iterator().hasNext()) {
					failedDirectDownload = true;
				} else {
					for(Path path : resolved) {
						files.add(path.toFile());
					}
				}
			}

			DependencyHandler deps = this.project.getDependencies();

			if(failedDirectDownload) {
				Dependency dependency = deps.create(library.name);
				files.addAll(AmalgIO.resolve(this.project, List.of(dependency)));
			}

			Dependency sources = deps.create(library.name + ":sources");
			List<Path> resolvedSources;
			try {
				resolvedSources = AmalgIO.resolveSources(this.project, List.of(sources));
			} catch(ResolveException e) {
				resolvedSources = List.of();
			}

			for(Path file : resolvedSources) {
				AmalgIO.SOURCES.add(file.toRealPath());
				files.add(file.toFile());
			}
		}


		return files.stream().map(File::toPath).toList();
	}
}
