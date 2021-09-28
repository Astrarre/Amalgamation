package io.github.astrarre.amalgamation.gradle.dependencies.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import net.devtech.zipio.processors.zip.PostZipProcessor;
import net.devtech.zipio.stage.LinkedProcessTransform;
import net.devtech.zipio.stage.ZipTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public interface ZipProcessable {
	default Iterable<Path> resolvePaths() throws IOException {
		ZipProcess process = this.process();
		process.execute();
		return process.getOutputs();
	}

	ZipProcess process();

	static List<ZipTransform> add(Project project, ZipProcessBuilder process, Dependency dependency, UnaryOperator<Path> getOutput) {
		return add(project, process, Collections.singleton(dependency), getOutput);
	}

	static List<ZipTransform> add(Project project, ZipProcessBuilder process, Iterable<Dependency> dependencies, UnaryOperator<Path> getOutput) {
		List<Dependency> deps = new ArrayList<>();
		List<ZipTransform> outputs = new ArrayList<>();
		for(Dependency dependency : dependencies) {
			if(dependency instanceof ZipProcessable p) {
				outputs.add(process.linkProcess(p.process(), getOutput));
			} else {
				deps.add(dependency);
			}
		}
		for(File file : AmalgIO.resolve(project, deps)) {
			Path path = file.toPath();
			outputs.add(process.addZip(path, getOutput.apply(path)));
		}
		return outputs;
	}
}
