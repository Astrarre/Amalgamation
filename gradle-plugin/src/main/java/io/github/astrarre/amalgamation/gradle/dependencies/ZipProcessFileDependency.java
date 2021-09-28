package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.files.ZipProcessCachedFile;
import net.devtech.zipio.processes.ZipProcess;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class ZipProcessFileDependency extends AbstractSelfResolvingDependency implements ZipProcessable {
	final ZipProcessCachedFile file;
	final ZipProcess process;

	public ZipProcessFileDependency(Project project, String group, String name, String version, ZipProcessCachedFile file) {
		super(project, group, name, version);
		this.file = file;
		try {
			this.process = file.createProcess();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Dependency copy() {
		return new ZipProcessFileDependency(this.project, this.group, this.name, this.version, this.file);
	}

	@Override
	public Iterable<Path> resolvePaths() {
		return this.file.getOutput();
	}

	@Override
	public ZipProcess process() {
		return this.process;
	}
}
