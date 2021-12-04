package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class SingleRemapDependency extends ZipProcessDependency {
	final Object dependency;
	final RemapDependency remap;
	final AmalgDirs dirs;
	final AmalgRemapper remapper;
	final boolean isClasspath;

	public SingleRemapDependency(Project project,
			Object dependency, RemapDependency remap, AmalgDirs dirs,
			AmalgRemapper remapper, boolean classpath) {
		super(project);
		this.dependency = dependency;
		this.remap = remap;
		this.dirs = dirs;
		this.remapper = remapper;
		this.isClasspath = classpath;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putString("remap", StandardCharsets.UTF_8);
		this.hashDep(hasher, this.dependency);
		hasher.putBytes(this.remap.config.getMappingsHash());
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.dirs.remaps(this.project).resolve(AmalgIO.b64(hash) + ".srd");
	}

	public void appendOutputs(ZipProcessBuilder builder) throws IOException {
		if(!this.isClasspath) {
			this.artifacts(this.dependency, false)
					.stream()
					.map(this::transform)
					.forEach(builder::addProcessed);
		}
	}

	@Override
	public void appendToProcess(ZipProcessBuilder builder, boolean isOutdated) throws IOException {
		List<TaskTransform> apply = this.apply(builder, this.dependency, this::transform);
		if(this.isOutdated() && !this.isClasspath) {
			for(TaskTransform task : apply) {
				task.setPreEntryProcessor(o -> this.remapper.remap());
				task.setPostZipProcessor(o -> this.remapper.remap());
			}
		} else {
			for(TaskTransform task : apply) {
				task.setPreEntryProcessor(o -> this.remapper.classpath());
				task.setZipFilter(o -> ResourceZipFilter.SKIP);
			}
			this.appendOutputs(builder);
		}
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		this.remap.getArtifacts();
		this.appendOutputs(process);
	}

	@NotNull
	private Artifact transform(Artifact o) {
		return o.deriveMaven(this.dirs.remaps(this.project), this.getCurrentHash());
	}
}
