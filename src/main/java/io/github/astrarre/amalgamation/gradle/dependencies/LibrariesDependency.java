package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import java.nio.file.Path;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class LibrariesDependency extends AmalgamationDependency {
	public final String version;
	/**
	 * defaults to your .minecraft installation, if not found, uses amalgamation cache
	 */
	public String librariesDirectory;
	/**
	 * states whether to include natives in libraries
	 */
	public LauncherMeta.NativesRule rule = LauncherMeta.NativesRule.ALL_NON_NATIVES;
	/**
	 * whether this dependency should download the dependencies on its own, rather than relying on the IDE to do it. This should only really be used
	 * when transforming library dependencies.
	 */
	public boolean download;

	public LibrariesDependency(Project project, String version) {
		super(project);
		this.version = version;
		this.librariesDirectory = MinecraftAmalgamationGradlePlugin.getLibrariesCache(project);
	}

	@Override
	protected Set<Artifact> resolveArtifacts() throws IOException {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		final Path dir = Paths.get(this.librariesDirectory);
		List<LauncherMeta.Library> libraries = meta.getVersion(this.version).getLibraries();
		Set<Artifact> files = new HashSet<>();

		for(LauncherMeta.Library library : libraries) {
			if(!this.download) {
				files.addAll(this.artifacts(library.name, true));
				continue;
			}

			boolean failedDirectDownload = false; // use maven as fallback incase using the URL does not work (maybe mojang servers down?)
			for(LauncherMeta.HashedURL dependency : library.evaluateAllDependencies(this.rule)) {
				Path jar = dir.resolve(dependency.path);
				HashedURLDependency dep = new HashedURLDependency(this.project, dependency);
				dep.output = jar;
				dep.isOptional = true;
				var resolved = dep.getArtifacts();
				if(resolved.isEmpty()) {
					failedDirectDownload = true;
				} else {
					files.addAll(resolved);
				}
			}

			DependencyHandler deps = this.project.getDependencies();

			if(failedDirectDownload) {
				files.addAll(this.artifacts(library.name));
			} else {
				Dependency sources = deps.create(library.name + ":sources");
				List<Path> resolvedSources;
				try {
					resolvedSources = AmalgIO.resolveSources(this.project, List.of(sources));
				} catch(ResolveException e) {
					resolvedSources = List.of();
				}

				for(Path file : resolvedSources) {
					files.add(new Artifact.File(
							this.project,
							sources.getGroup(),
							sources.getName(),
							sources.getVersion(), file,
							library.name.getBytes(StandardCharsets.UTF_8),
							Artifact.Type.MIXED
					));
				}
			}
		}
		return files;
	}
}
