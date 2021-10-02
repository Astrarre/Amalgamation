package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.ZipProcessCachedFile;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class CachedFileDependency extends AbstractSelfResolvingDependency implements ZipProcessable {
	final List<CachedFile> files = new ArrayList<>();
	public CachedFileDependency(Project project, String group, String name, String version) {
		super(project, group, name, version);
	}

	public CachedFileDependency add(CachedFile file) {
		this.files.add(file);
		return this;
	}

	@Override
	public Dependency copy() {
		CachedFileDependency dependency = new CachedFileDependency(this.project, this.group, this.name, this.version);
		dependency.files.addAll(this.files);
		return dependency;
	}

	@Override
	public Iterable<Path> resolvePaths() throws IOException {
		return Iterables.transform(this.files, CachedFile::getOutput);
	}

	@Override
	public ZipProcess process() {
		if(files.isEmpty()) {
			return ZipProcess.empty();
		} else if(files.size() == 1) {
			CachedFile file = this.files.get(0);
			if(file instanceof ZipProcessCachedFile z) {
				try {
					return z.process();
				} catch(IOException e) {
					throw U.rethrow(e);
				}
			}
		}

		ZipProcessBuilder process = ZipProcess.builder();
		for(CachedFile file : this.files) {
			if(file instanceof ZipProcessCachedFile z) {
				try {
					process.linkProcess(z.process(), p -> z.path());
				} catch(IOException e) {
					throw U.rethrow(e);
				}
			} else {
				process.addProcessed(file.getOutput());
			}
		}

		return process;
	}
}
