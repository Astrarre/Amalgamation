package io.github.astrarre.amalgamation.gradle.files;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.splitter.ClasspathSplitterDir;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;

public class SplitClasspathProvider implements Supplier<FileCollection> {
	private final Project project;
	private final Action<ConfigurableFileCollection> config;
	private final String[] platforms;

	public SplitClasspathProvider(Project amalgamation, Action<ConfigurableFileCollection> config, String... platforms) {
		this.project = amalgamation;
		this.config = config;
		this.platforms = platforms;
	}

	@Override
	public FileCollection get() {
		ConfigurableFileCollection classpath = this.project.files();
		this.config.execute(classpath);
		Path globalCache = BaseAmalgamationImpl.globalCache(this.project.getGradle());
		String globalCachePath = globalCache.toAbsolutePath().toString();
		Path projectCache = BaseAmalgamationImpl.projectCache(this.project);
		ConfigurableFileCollection split = this.project.files();
		List<File> dirs = new ArrayList<>();

		for (File file : classpath) {
			if (!file.exists()) {
				continue;
			}
			if (file.isDirectory()) {
				dirs.add(file);
			} else {
				String absolute = file.getAbsolutePath();
				Path splitDir;
				if (absolute.startsWith(globalCachePath)) { // global file for splitting
					splitDir = globalCache.resolve("splits");
				} else {
					splitDir = projectCache.resolve("splits");
				}
				Path dest = splitDir.resolve(file.getName());
				int counter = 0;
				while (Files.exists(dest)) {
					dest = splitDir.resolve(counter + "-" + file.getName());
				}

				ClasspathSplitterDir splitter = new ClasspathSplitterDir(dest, this.project,
						Collections.singletonList(file.toPath()),
						Arrays.asList(this.platforms));
				split.from(splitter.getPath());
			}
		}

		if (!dirs.isEmpty()) {
			ClasspathSplitterDir splitter = new ClasspathSplitterDir(BaseAmalgamationImpl.projectCache(this.project).resolve("dest.jar"),
					this.project,
					Lists.transform(dirs, File::toPath),
					Arrays.asList(this.platforms));
			split.from(splitter.getPath());
		}

		return split;
	}
}
