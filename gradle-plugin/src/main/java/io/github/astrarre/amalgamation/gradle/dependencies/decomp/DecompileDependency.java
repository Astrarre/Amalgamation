package io.github.astrarre.amalgamation.gradle.dependencies.decomp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

public class DecompileDependency extends CachedDependency {
	private static int TASK_NAME_C = 0;
	private final List<Object> classpath = new ArrayList<>();
	private final Artifact input;
	private final Set<Artifact> everythingElse;
	private final LoomDecompiler decompiler;
	private AmalgDirs cache = AmalgDirs.ROOT_PROJECT;
	private Artifact javadocs;
	private String optionalTask;
	private String namespace;

	public DecompileDependency(Project project, Object dependency, LoomDecompiler decompiler) {
		super(project);
		this.decompiler = decompiler;

		Set<Artifact> artifacts = this.artifacts(dependency, true);

		Artifact input = null;
		for(Artifact artifact : artifacts) {
			if(artifact.type.containsClasses()) {
				if(input != null) {
					throw new UnsupportedOperationException("cannot decompile multiple jars, disable transitive dependencies");
				}
				input = artifact;
			}
		}

		if(input == null) {
			throw new UnsupportedOperationException("all inputs were sources or resources jar, cannot decompile!");
		}

		this.input = input;
		this.everythingElse = new HashSet<>(artifacts);
		this.everythingElse.remove(input);
	}

	public void setJavadocs(Object dependency, String namespace) {
		List<Artifact> javadocs = artifacts(dependency, true).stream().filter(a -> a.type != Artifact.Type.SOURCES).toList();
		this.javadocs = Iterables.getOnlyElement(javadocs);
		this.namespace = namespace;
	}

	public void classpath(Object dep) {
		this.classpath.add(dep);
	}

	public void cacheGlobally() {
		this.cache = AmalgDirs.GLOBAL;
	}

	public void optionalTask(String taskName) {
		this.optionalTask = taskName;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putBytes(this.input.hash);
		if(this.javadocs != null) {
			hasher.putBytes(this.javadocs.hash);
			hasher.putString(this.namespace, StandardCharsets.UTF_8);
		}
		hasher.putString(this.decompiler.name(), StandardCharsets.UTF_8);
		for(Object o : this.classpath) {
			this.hashDep(hasher, o);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.input.deriveMaven(this.cache.decomps(this.project), hash).path;
	}

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		Artifact lineMapped = this.input.deriveMaven(this.cache.decomps(this.project), this.getCurrentHash());
		Artifact sources = this.input.deriveMaven(this.cache.decomps(this.project), this.getCurrentHash(), Artifact.Type.SOURCES);
		if(isOutdated) {
			TaskContainer tasks = project.getTasks();
			GenerateSourcesTask task;
			try {
				task = tasks.create(this.optionalTask == null ? "decompile" + TASK_NAME_C++ : this.optionalTask, GenerateSourcesTask.class, this.decompiler);
			} catch(InvalidUserDataException a) {
				var set = new HashSet<>(this.everythingElse);
				set.add(this.input);
				throw new Uncached(set);
			}
			task.getJavadocNamespace().set(this.namespace);
			task.getInputJar().set(this.input.path.toFile());
			for(Object o : this.classpath) {
				task.getClasspath().from(this.artifacts(o, true).stream().map(a -> a.path).map(Path::toFile).toArray());
			}
			task.getOutputDestination().set(sources.getPath().toFile());
			task.getLineMappedDestination().set(lineMapped.getPath().toFile());
			if(this.javadocs != null) {
				task.getMappingsFile().set(this.javadocs.getPath().toFile());
			}
			if(this.optionalTask == null) {
				task.run();
				task.setEnabled(false);
			} else {
				task.appendParallelSafeAction(t -> {
					try {
						this.writeHash();
					} catch(IOException e) {
						throw U.rethrow(e);
					}
				});
				var set = new HashSet<>(this.everythingElse);
				set.add(this.input);
				throw new Uncached(set);
			}
		} else {
			if(this.optionalTask != null) {
				TaskContainer tasks = project.getTasks();
				tasks.create(this.optionalTask).doFirst(t -> {
					this.logger.lifecycle("A cached decompilation already exists for this artifact, running this task now does nothing!");
				});
			}
		}
		var set = new HashSet<>(this.everythingElse);
		set.add(lineMapped);
		set.add(sources);
		return set;
	}
}
