package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.dependencies.filtr.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;

public abstract class ZipProcessDependency extends CachedDependency implements ZipProcessable {
	public ZipProcessDependency(Project project, String group, String name, String version) {
		super(project, group, name, version);
	}

	@Override
	public ZipProcess process() throws IOException {
		// todo ZipProcess in-memory caching
		// todo cache TR classpaths at some point:tm:
		ZipProcessBuilder builder = ZipProcess.builder();
		builder.defaults().setZipFilter(p -> ResourceZipFilter.FILTER);
		boolean isOutdated = this.isOutdated();
		this.add(builder, this.getPath(), isOutdated);
		builder.afterExecute(() -> {
			this.after(isOutdated);
		});
		return builder;
	}

	protected void after(boolean isOutdated) {
		if(isOutdated) {
			try {
				this.writeHash();
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
	}

	protected static OutputTag tag(Path path) {
		return new OutputTag(path);
	}

	@Override
	protected Iterable<Path> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		try(Clock ignore = new Clock("Processed " + this + " in %sms", this.getLogger())) {
			ZipProcess process = this.process();
			process.execute();
			List<Path> paths = new ArrayList<>();
			for(OutputTag output : process.getOutputs()) {
				if(output.path != null) {
					paths.add(output.path);
				}
			}
			return paths;
		}
	}

	@Override
	public Iterable<Path> resolvePaths() throws IOException {
		return super.resolvePaths();
	}

	// todo by default add resource zip filter

	/**
	 * Remember to update your hash here if u need it
	 */
	protected abstract void add(ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException;
}
