package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.zipio.OutputTag;
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

	boolean resolving;

	@Override
	public void appendToProcess(ZipProcessBuilder builder, boolean isOutdated) throws IOException {
		if(!isOutdated || this.isClasspath) {
			List<TaskTransform> apply = this.apply(builder, this.dependency, o -> OutputTag.INPUT);
			for(TaskTransform task : apply) {
				task.setPreEntryProcessor(o -> this.remapper.classpath());
			}
			this.appendOutputs(builder);
		} else {
			List<TaskTransform> apply;
			try {
				this.resolving = true;
				apply = this.apply(builder, this.dependency, this::transform);
			} finally {
				this.resolving = false;
			}
			for(TaskTransform task : apply) {
				Map<Object, AmalgRemapper.Remap> reMap = new HashMap<>();
				task.setPreEntryProcessor(o -> reMap.computeIfAbsent(o, $ -> this.remapper.remap()));
				task.setPostZipProcessor(o -> reMap.computeIfAbsent(o, $ -> this.remapper.remap()));
			}
		}
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		this.remap.getArtifacts();
		this.appendOutputs(process);
	}

	@NotNull
	private Artifact transform(Artifact o) {
		Artifact artifact = this.remap.artifactSet.get(o);
		if(artifact != null) {
			return artifact;
		}
		return o.deriveMavenMixHash(this.dirs.remaps(this.project), this.remap.config.getMappingsHash());
	}

	@Override
	protected boolean validateArtifact(Artifact artifact) {
		return !this.resolving || this.remap.artifactSet.putIfAbsent(artifact, this.transform(artifact)) == null;
	}
}
