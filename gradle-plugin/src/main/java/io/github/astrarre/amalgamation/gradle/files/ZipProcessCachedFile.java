package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.utils.Clock;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import org.gradle.api.Project;

public abstract class ZipProcessCachedFile extends CachedFile {
	public final Project project;
	public ZipProcessCachedFile(Path output, Project project) {
		super(output);
		this.project = project;
	}

	public ZipProcess process() throws IOException {
		if(!this.isOutdated()) {
			ZipProcessBuilder process = ZipProcess.builder();
			process.addProcessed(this.path());
			return process;
		}

		ZipProcessBuilder process = ZipProcess.builder();
		this.init(process, this.path());
		process.afterExecute(this::updateHash); // todo pre-execute for timing
		return process;
	}


	/**
	 * Set processing settings, eg {@link ZipProcessBuilder#setEntryProcessor(ZipEntryProcessor)}
	 */
	public abstract void init(ZipProcessBuilder process, Path outputFile) throws IOException;

	@Override
	protected void write(Path output) throws IOException {
		try(Clock ignored = new Clock(this + " in %dms", this.project.getLogger())) {
			ZipProcess process = this.process();
			process.execute();
		}
	}
}
