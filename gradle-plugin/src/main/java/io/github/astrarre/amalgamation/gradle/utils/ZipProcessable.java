package io.github.astrarre.amalgamation.gradle.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Streams;
import io.github.astrarre.amalgamation.gradle.dependencies.filters.SourcesOutput;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public interface ZipProcessable {
	ZipProcess process() throws IOException;

	static Stream<OutputTag> getOutputs(Project project, Dependency dependency) throws IOException {
		if(dependency instanceof ZipProcessable z) {
			return Streams.stream(z.process().getOutputs());
		} else {
			List<Dependency> deps = List.of(dependency);
			return Stream.concat(AmalgIO.resolve(project, deps).stream().map(File::toPath).map(OutputTag::new),
					AmalgIO.resolveSources(project, deps).stream().map(SourcesOutput::new));
		}
	}

	static List<TaskTransform> apply(Project project, ZipProcessBuilder process, Dependency dependency, UnaryOperator<OutputTag> getOutput)
			throws IOException {
		return apply(project, process, Collections.singleton(dependency), getOutput);
	}

	static List<TaskTransform> apply(Project project, ZipProcessBuilder process, Iterable<Dependency> dependencies, UnaryOperator<OutputTag> getOutput)
			throws IOException {
		List<Dependency> deps = new ArrayList<>();
		List<TaskTransform> transforms = new ArrayList<>();
		for(Dependency dependency : dependencies) {
			if(dependency instanceof ZipProcessable p) {
				transforms.add(process.linkProcess(p.process(), getOutput));
			} else {
				deps.add(dependency);
			}
		}
		for(File file : AmalgIO.resolve(project, deps)) {
			Path path = file.toPath();
			transforms.add(process.addZip(path, getOutput.apply(new OutputTag(path))));
		}

		for(Path sources : AmalgIO.resolveSources(project, dependencies)) {
			transforms.add(process.addZip(sources, getOutput.apply(new SourcesOutput(sources))));
		}
		return transforms;
	}
}
