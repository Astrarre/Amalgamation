package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import io.github.astrarre.amalgamation.gradle.dependencies.filters.Filters;
import io.github.astrarre.amalgamation.gradle.dependencies.filters.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.dependencies.filters.SourcesOutput;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public abstract class ZipProcessDependency extends CachedDependency implements ZipProcessable {
	public ZipProcessDependency(Project project, String group, String name, String version) {
		super(project, group, name, version);
	}

	@Override
	public ZipProcess process() throws IOException {
		// todo ZipProcess piping whereby a single process can have multiple dependents
		// todo cache TR classpaths at some point:tm:
		ZipProcessBuilder builder = ZipProcess.builder();
		builder.defaults().setZipFilter(p -> ResourceZipFilter.FILTER);
		boolean isOutdated = this.isOutdated();
		TaskInputResolver resolver = (dependencies, tag) -> ZipProcessable.apply(this.project, builder, dependencies, tag);
		this.add(resolver, builder, this.getPath(), isOutdated);
		builder.afterExecute(() -> {
			for(OutputTag output : builder.getOutputs()) {
				if(output instanceof SourcesOutput s) {
					try {
						AmalgIO.SOURCES.add(s.getVirtualPath().toRealPath());
					} catch(IOException e) {
						throw U.rethrow(e);
					}
				}
			}

			this.after(isOutdated);
		});

		return builder;
	}

	public boolean requiresSources() {
		return true;
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

	protected static OutputTag tag(OutputTag tag, Path path) {
		return Filters.from(tag, path);
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

	/**
	 * Remember to update your hash here if u need it
	 */
	protected abstract void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException;

	public List<TaskTransform> apply(ZipProcessBuilder builder, Iterable<Dependency> dependencies, UnaryOperator<OutputTag> tag)
			throws IOException {
		return ZipProcessable.apply(this.project, builder, dependencies, tag);
	}

	public List<TaskTransform> apply(ZipProcessBuilder builder, Dependency dep, UnaryOperator<OutputTag> tag)
			throws IOException {
		return ZipProcessable.apply(this.project, builder, dep, tag);
	}

	public interface TaskInputResolver {
		List<TaskTransform> apply(Iterable<Dependency> dependencies, UnaryOperator<OutputTag> tag) throws IOException;

		default List<TaskTransform> apply(Dependency dependency, UnaryOperator<OutputTag> tag) throws IOException {
			return this.apply(List.of(dependency), tag);
		}
	}
}
