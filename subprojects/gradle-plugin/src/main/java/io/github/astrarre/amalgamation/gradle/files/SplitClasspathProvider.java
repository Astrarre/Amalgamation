package io.github.astrarre.amalgamation.gradle.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import io.github.astrarre.amalgamation.gradle.dependencies.AbstractSelfResolvingDependency;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.splitter.ClasspathSplitterDir;
import io.github.astrarre.amalgamation.utils.CachedFile;
import io.github.astrarre.merger.Mergers;
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

			switch (this.get(file, Arrays.asList(this.platforms))) {
			case SPLIT:
				String absolute = file.getAbsolutePath();
				Path splitDir;
				if (absolute.startsWith(globalCachePath)) { // global file for splitting
					splitDir = globalCache.resolve("splits");
				} else {
					splitDir = projectCache.resolve("splits");
				}

				Path dest = CachedFile.forHash(splitDir, sink -> {
					sink.putUnencodedChars(file.getAbsolutePath());
					sink.putLong(file.lastModified());
				}).resolve(file.getName());
				ClasspathSplitterDir splitter = new ClasspathSplitterDir(dest, this.project,
						Collections.singletonList(file.toPath()),
						Arrays.asList(this.platforms));
				split.from(splitter.getPath());
				break;
			case APPEND:
				split.from(file);
				break;
			case COMBINE:
				dirs.add(file);
				break;
			default:
				break;
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

	public SplitBehavior get(File file, List<String> platforms) {
		if(file.isDirectory()) {
			return SplitBehavior.COMBINE;
		}
		try(FileSystem system = FileSystems.newFileSystem(file.toPath(), null)) {
			if (!Files.exists(system.getPath(Mergers.RESOURCES_MARKER_FILE))) {
				Path path = system.getPath(Mergers.MERGER_META_FILE);
				if (Files.exists(path)) {
					Properties properties = new Properties();
					properties.load(Files.newInputStream(path));
					if (properties.getProperty(Mergers.RESOURCES, "false").equals("true")) {
						String property = properties.getProperty(Mergers.PLATFORMS);
						if (Arrays.asList(property.split(",")).containsAll(platforms)) {
							return SplitBehavior.APPEND;
						} else {
							return SplitBehavior.SKIP;
						}
					}
					return SplitBehavior.SPLIT;
				}
			}
			return SplitBehavior.APPEND;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public enum SplitBehavior {
		COMBINE, // for directories
		SKIP, // for invalid resources or merger files that are bad
		APPEND, // for valid resources
		SPLIT // everything else
	}
}
