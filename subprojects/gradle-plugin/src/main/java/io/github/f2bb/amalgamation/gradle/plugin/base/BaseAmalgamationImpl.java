package io.github.f2bb.amalgamation.gradle.plugin.base;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.github.f2bb.amalgamation.gradle.dependencies.MergerDependency;
import io.github.f2bb.amalgamation.gradle.splitter.ClasspathSplitterDir;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

public class BaseAmalgamationImpl implements BaseAmalgamation {
	public static final String OPERATING_SYSTEM;
	public static final ExecutorService SERVICE = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> {
		ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		thread.setDaemon(true);
		return thread;
	}, null, true);

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			OPERATING_SYSTEM = "windows";
		} else if (osName.contains("mac")) {
			OPERATING_SYSTEM = "osx";
		} else {
			OPERATING_SYSTEM = "linux";
		}

	}

	protected final Project project;

	public BaseAmalgamationImpl(Project project) {this.project = project;}

	@Override
	public Dependency merge(Action<MergerDependency> configuration) {
		MergerDependency config = new MergerDependency(this.project);
		configuration.execute(config);
		return config;
	}

	@Override
	public Provider<FileCollection> splitClasspath(Action<ConfigurableFileCollection> config, String... platforms) {
		return this.project.provider(() -> {
			ConfigurableFileCollection classpath = this.project.files();
			config.execute(classpath);
			Path path = CachedFile.globalCache(this.project.getGradle()).resolve("splits");

			ConfigurableFileCollection split = this.project.files();
			List<File> dirs = new ArrayList<>();

			for (File file : classpath) {
				if(!file.exists()) continue;
				if (file.isDirectory()) {
					dirs.add(file);
				} else {
					Path dest = CachedFile.forHash(path, sink -> {
						sink.putUnencodedChars(file.getName());
						sink.putLong(file.lastModified());
					});
					dest = dest.resolve(file.getName());
					ClasspathSplitterDir splitter = new ClasspathSplitterDir(dest, this.project, Collections.singletonList(file.toPath()), Arrays.asList(platforms));
					split.from(splitter.getPath());
				}
			}

			if(!dirs.isEmpty()) {
				ClasspathSplitterDir splitter = new ClasspathSplitterDir(CachedFile.projectCache(this.project).resolve("dir.jar"),
						this.project,
						Lists.transform(dirs, File::toPath),
						Arrays.asList(platforms));
				split.from(splitter.getPath());
			}

			return split;
			//return .toFile();

		});
	}

}
