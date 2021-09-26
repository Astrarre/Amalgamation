package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.files.CASMergedFile;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class CASMergedDependency extends AbstractSingleFileSelfResolvingDependency {
	final CachedFile<?> file;

	public CASMergedDependency(Project project,
			String version,
			CASMerger.Handler handler,
			int settings,
			boolean only,
			Supplier<File> client,
			Supplier<File> server) {
		super(project, "net.minecraft", "cas-merged", version);
		Path jar = AmalgIO.globalCache(project.getGradle()).resolve(version).resolve("merged.jar");
		this.file = new CASMergedFile(jar, project, version, handler, settings, only, client, server);
	}

	public CASMergedDependency(Project project, String group, String name, String version, CachedFile<?> file) {
		super(project, group, name, version);
		this.file = file;
	}

	@Override
	public Dependency copy() {
		return new CASMergedDependency(this.project, this.group, this.name, this.version, this.file);
	}

	@Override
	protected Path resolvePath() {
		return this.file.getPath();
	}

	public static class Config {
		final DependencyHandler project;
		public String version;
		public CASMerger.Handler handler = CASMerger.FABRIC;
		public int classReaderSettings = 0;
		public boolean checkForServerOnly = false;
		public Dependency client;
		public Dependency server;

		public Config(Project project) {this.project = project.getDependencies();}

		public Config client(Object client, Closure<ModuleDependency> config) {
			this.client = this.project.create(client, config);
			return this;
		}

		public Config server(Object server, Closure<ModuleDependency> config) {
			this.server = this.project.create(server, config);
			return this;
		}

		public Config client(Object client) {
			this.client = this.project.create(client);
			return this;
		}

		public Config server(Object server) {
			this.server = this.project.create(server);
			return this;
		}
	}
}
