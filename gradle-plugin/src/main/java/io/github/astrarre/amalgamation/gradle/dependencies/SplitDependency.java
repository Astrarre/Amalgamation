package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.common.hash.Hasher;
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
		super(project);
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

	Artifact.File artifact(Path dest, Artifact.Type type) {
		return new Artifact.File(
				this.project,
				this.dependency.getGroup(),
				this.dependency.getName() + "-" + type.name().toLowerCase(Locale.ROOT),
				this.dependency.getVersion(),
				dest,
				this.getCurrentHash(),
				type
		);
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		Files.createDirectories(resolvedPath);
		AmalgIO.createFile(resolvedPath.resolve("resources.jar.rss_marker"));
		Artifact cls = this.artifact(resolvedPath.resolve("classes.jar"), Artifact.Type.MIXED);
		Artifact rss = this.artifact(resolvedPath.resolve("resources.jar"), Artifact.Type.RESOURCES);
		Artifact sources = this.artifact(resolvedPath.resolve("sources.jar"), Artifact.Type.SOURCES);
		if(isOutdated) {
			ZipTag clsTag = process.createZipTag(cls), rssTag = process.createZipTag(rss), sourcesTag = process.createZipTag(sources);
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
			resolver.apply(this.dependency, o -> OutputTag.INPUT);
		} else {
			process.addProcessed(cls);
			process.addProcessed(rss);
			process.addProcessed(sources);
		}
	}

}
