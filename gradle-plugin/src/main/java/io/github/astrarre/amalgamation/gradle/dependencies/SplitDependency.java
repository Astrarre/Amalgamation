package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.ZipTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class SplitDependency extends ZipProcessDependency {
	public final Dependency dependency;
	public Path outputDir;
	public SplitDependency(Project project, String version, Dependency dependency) {
		super(project, "io.github.astrarre.amalgamation", "split-dependency", version);
		this.dependency = dependency;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.dependency);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return this.outputDir;
	}

	@Override
	protected void add(ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		Files.createDirectories(resolvedPath);
		AmalgIO.createFile(resolvedPath.resolve("resources.jar.rss_marker"));
		Path cls = resolvedPath.resolve("classes.jar"), rss = resolvedPath.resolve("resources.jar"), sources = resolvedPath.resolve("sources.jar");
		if(isOutdated) {
			ZipTag clsTag = process.createZipTag(cls), rssTag = process.createZipTag(new ResourcesOutput(rss)), sourcesTag = process.createZipTag(sources);
			ZipProcessable.add(this.project, process, this.dependency, p -> OutputTag.INPUT);
			process.setEntryProcessor(buffer -> {
				String path = buffer.path();
				if(path.endsWith(".class") || path.contains("META-INF")) {
					buffer.copyTo(path, clsTag);
				} else if(path.endsWith(".java")) {
					buffer.copyTo(path, sourcesTag);
				} else {
					buffer.copyTo(path, rssTag);
				}
				return ProcessResult.HANDLED;
			});
		} else {
			process.addProcessed(cls);
			process.addProcessed(new ResourcesOutput(rss));
			process.addProcessed(sources);
		}
	}

	public static class ResourcesOutput extends OutputTag {
		public ResourcesOutput(Path path) {
			super(path);
		}
	}
}
