package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.github.astrarre.amalgamation.gradle.dependencies.util.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;

public abstract class ZipProcessDependency extends CachedDependency {
	public ZipProcessDependency(Project project) {
		super(project);
	}

	public ZipProcess process() throws IOException {
		// todo ZipProcess piping whereby a single process can have multiple dependents
		// todo cache TR classpaths at some point:tm:
		ZipProcessBuilder builder = ZipProcess.builder();
		builder.defaults().setZipFilter(p -> ResourceZipFilter.FILTER);
		boolean isOutdated = this.isOutdated();
		TaskInputResolver resolver = (dependencies, tag) -> this.apply(builder, dependencies, tag);
		this.add(resolver, builder, this.getPath(), isOutdated);
		builder.afterExecute(() -> this.after(isOutdated));
		return builder;
	}

	public void appendToProcess(ZipProcessBuilder builder, boolean isOutdated) throws IOException {
		TaskInputResolver resolver = (dependencies, tag) -> this.apply(builder, dependencies, tag);
		this.add(resolver, builder, this.getPath(), isOutdated);
		builder.afterExecute(() -> this.after(isOutdated));
	}

	public void appendToProcess(ZipProcessBuilder builder) throws IOException {
		this.appendToProcess(builder, this.isOutdated());
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

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		try(Clock ignore = new Clock("Processed " + this + " in %sms", this.logger)) {
			ZipProcess process = this.process();
			process.execute();
			Set<Artifact> paths = new HashSet<>();
			for(OutputTag output : process.getOutputs()) {
				if(!(output instanceof Artifact a)) {
					throw new IllegalArgumentException("Output of type other than Artifact! " + output.getClass());
				}
				if(output.path != null) {
					paths.add(a);
				}
			}
			return paths;
		}
	}

	/**
	 * Remember to update your hash here if u need it
	 */
	protected abstract void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException;

	public List<TaskTransform> apply(ZipProcessBuilder builder, Object dep, Function<Artifact, OutputTag> tag)
			throws IOException {
		return this.apply(builder, List.of(dep), tag);
	}

	public interface TaskInputResolver {
		List<TaskTransform> apply(Iterable<Object> dependencies, Function<Artifact, OutputTag> tag) throws IOException;

		default List<TaskTransform> apply(Object dependency, Function<Artifact, OutputTag> tag) throws IOException {
			return this.apply(List.of(dependency), tag);
		}
	}

	public List<TaskTransform> apply(ZipProcessBuilder process, Iterable<Object> dependencies, Function<Artifact, OutputTag> output)
			throws IOException {
		List<TaskTransform> transforms = new ArrayList<>();
		for(Object dep : dependencies) {
			if(dep instanceof ZipProcessDependency p) {
				transforms.add(process.linkProcess(p.process(), o -> output.apply((Artifact) o)));
			} else {
				for(Artifact artifact : this.artifacts(dep, true)) {
					if(this.validateArtifact(artifact)) {
						transforms.add(process.addZip(artifact.path, output.apply(artifact)));
					}
				}
			}
		}
		return transforms;
	}

	protected boolean validateArtifact(Artifact artifact) {
		return true;
	}
}
