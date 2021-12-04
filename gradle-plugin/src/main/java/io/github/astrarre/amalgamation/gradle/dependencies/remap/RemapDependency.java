package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class RemapDependency extends ZipProcessDependency {
	public final RemapDependencyConfig config = new RemapDependencyConfig(this);

	public RemapDependency(Project project) {
		super(project);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.config.hash(hasher);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.getDir().remaps(this.project).resolve(AmalgIO.b64(hash));
	}

	public SingleRemapDependency createDep(Object dep, AmalgDirs dirs, boolean classpath) {
		return new SingleRemapDependency(this.project, dep, this, dirs, this.config.getRemapper(), classpath);
	}

	AmalgDirs getDir() {
		return this.config.inputsLocal.isEmpty() ? AmalgDirs.GLOBAL : AmalgDirs.ROOT_PROJECT;
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		List<SingleRemapDependency> dependencies = new ArrayList<>();
		dependencies.addAll(this.config.inputsGlobal);
		dependencies.addAll(this.config.inputsLocal);
		List<SingleRemapDependency> classpath = new ArrayList<>();
		for(Object dependency : this.config.getClasspath()) {
			classpath.add(this.createDep(dependency, AmalgDirs.GLOBAL, true));
		}

		if(isOutdated) {
			List<Mappings.Namespaced> mappings = new ArrayList<>();
			for(MappingTarget mapping : this.config.getMappings()) {
				mappings.add(mapping.read());
			}
			this.config.getRemapper().init(mappings);
			for(SingleRemapDependency dependency : dependencies) {
				dependency.appendToProcess(process);
			}
			for(SingleRemapDependency dependency : classpath) {
				dependency.appendToProcess(process);
			}
		} else {
			for(SingleRemapDependency dependency : dependencies) {
				dependency.appendOutputs(process);
			}
		}
	}
}
