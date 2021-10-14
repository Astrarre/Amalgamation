package io.github.astrarre.amalgamation.gradle.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public interface ZipProcessable {
	ZipProcess process() throws IOException;

	static List<TaskTransform> add(Project project, ZipProcessBuilder process, Dependency dependency, UnaryOperator<OutputTag> getOutput)
			throws IOException {
		return add(project, process, Collections.singleton(dependency), getOutput);
	}

	static List<TaskTransform> add(Project project, ZipProcessBuilder process, Iterable<Dependency> dependencies, UnaryOperator<OutputTag> getOutput)
			throws IOException {
		List<Dependency> deps = new ArrayList<>();
		List<TaskTransform> outputs = new ArrayList<>();
		for(Dependency dependency : dependencies) {
			if(dependency instanceof ZipProcessable p) {
				outputs.add(process.linkProcess(p.process(), getOutput));
			} else {
				deps.add(dependency);
			}
		}
		for(File file : AmalgIO.resolve(project, deps)) {
			Path path = file.toPath();
			outputs.add(process.addZip(path, getOutput.apply(new OutputTag(path))));
		}
		return outputs;
	}
}
