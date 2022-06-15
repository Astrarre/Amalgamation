package io.github.astrarre.amalgamation.gradle.dependencies.decomp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.AmalgamationDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.impl.util.FPInternal;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.TaskContainer;

/**
 * Decompiles given dependencies with a configurable decompiler
 */
public class DecompileDependency extends CachedDependency {
	private static int TASK_NAME_C = 0;
	private final Map<Artifact, DecompileOutput> artifactMap = new HashMap<>();
	private final List<SingleDecompileDependency> inputLocal = new ArrayList<>(), inputGlobal = new ArrayList<>();
	private final List<Object> classpath = new ArrayList<>();
	private final List<GenerateSourcesTask.JavadocEntry> javadocs = new ArrayList<>();
	private final List<Object> decompilerClasspath = new ArrayList<>();
	private AmalgDecompiler.Type<?> decompiler;
	private byte[] inputHash;
	private AmalgDirs amalgDirs = AmalgDirs.GLOBAL;
	private String optionalTask;

	public DecompileDependency(Project project) {
		super(project);
	}

	/**
	 * sets the fernflower decompiler as the decompiler to use
	 */
	public void fernflower(Object dependency) {
		if(this.decompiler != null) {
			throw new IllegalStateException("Cannot set multiple decompilers!");
		}
		this.decompiler = AmalgDecompiler.FERNFLOWER;
		this.decompilerClasspath.add(dependency);
	}

	public void fernflower(Object dependency, Closure<ModuleDependency> dep) {
		if(this.decompiler != null) {
			throw new IllegalStateException("Cannot set multiple decompilers!");
		}
		this.decompiler = AmalgDecompiler.FERNFLOWER;
		this.decompilerClasspath.add(this.of(dependency, dep));
	}

	/**
	 * Additional bits to add to the decompiler's classpath
	 */
	public void decompilerClasspath(Object dependency) {
		this.decompilerClasspath.add(dependency);
	}

	public void decompilerClasspath(Object dependency, Closure<ModuleDependency> dep) {
		this.decompilerClasspath.add(this.of(dependency, dep));
	}

	/**
	 * If using fabric fernflower or any forks, you can specify the mappings to pull javadocs from here
	 */
	public void javadocs(Object dependency, String namespace) {
		if(this.inputHash != null) {
			throw new IllegalArgumentException("Cannot configure after evaluation!");
		}
		File file = AmalgIO.resolveFile(this.project, dependency);
		this.javadocs.add(new GenerateSourcesTask.JavadocEntry(file, namespace));
	}

	/**
	 * A library dependency of the inputs to the decompilation task. This is used by the compiler to enhance decompilation
	 */
	public void classpath(Object dep) {
		this.classpath.add(dep);
	}

	public void classpath(Object dep, Closure<ModuleDependency> configure) {
		this.classpath.add(this.of(dep, configure));
	}

	/**
	 * if this option is enabled, a task with the given name is created, if the task is not executed, then the dependency wont decompile anything
	 *  but after the task has been run, the dependency will return the decompiled version.
	 *  This, in effect, allows you to optionally decompile the inputs. If you don't want to decompile, don't run the task, if u do, then u do.
	 */
	public void optionalTask(String taskName) {
		this.optionalTask = taskName;
	}

	/**
	 * Adds a dependency to be decompiled and cached locally (root project build folder)
	 * @return a dependency, that when evaluated triggers the decompilation of the whole task,
	 *  this dependency however, will only represent the outputs that correspond to the given dependency
	 */
	public Object inputLocal(Object dep) {
		SingleDecompileDependency dependency = new SingleDecompileDependency(dep);
		this.inputLocal.add(dependency);
		this.amalgDirs = AmalgDirs.ROOT_PROJECT;
		return dependency;
	}

	public Object inputLocal(Object dep, Closure<ModuleDependency> configure) {
		SingleDecompileDependency dependency = new SingleDecompileDependency(this.of(dep, configure));
		this.inputLocal.add(dependency);
		this.amalgDirs = AmalgDirs.ROOT_PROJECT;
		return dependency;
	}

	/**
	 * Adds a dependency to be decompiled and cached globally (gradle cache folder)
	 * @return a dependency, that when evaluated triggers the decompilation of the whole task,
	 *  this dependency however, will only represent the outputs that correspond to the given dependency
	 */
	public Object inputGlobal(Object dep) {
		SingleDecompileDependency dependency = new SingleDecompileDependency(dep);
		this.inputGlobal.add(dependency);
		return dependency;
	}

	public Object inputGlobal(Object dep, Closure<ModuleDependency> configure) {
		SingleDecompileDependency dependency = new SingleDecompileDependency(this.of(dep, configure));
		this.inputGlobal.add(dependency);
		return dependency;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putBytes(this.getInputHash());
		for(Object o : this.classpath) {
			this.hashDep(hasher, o);
		}
		for(Object o : this.decompilerClasspath) {
			hashDep(hasher, o);
		}
		for(SingleDecompileDependency o : this.inputLocal) {
			for(Artifact input : o.inputs) {
				hasher.putBytes(input.hash);
			}
		}
		for(SingleDecompileDependency o : this.inputGlobal) {
			for(Artifact input : o.inputs) {
				hasher.putBytes(input.hash);
			}
		}
	}

	@Override
	protected VirtualPath evaluatePath(byte[] hash) throws IOException {
		return this.amalgDirs.decomps(this.project).getFile(AmalgIO.b64(hash) + "g");
	}

	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws IOException {
		Objects.requireNonNull(this.decompiler, "no decompiler was set!");
		BiConsumer<AmalgDirs, List<SingleDecompileDependency>> populate = (dirs, dependencies) -> {
			for(SingleDecompileDependency dependency : dependencies) {
				for(Artifact input : dependency.inputs) {
					this.artifactMap.computeIfAbsent(input,
							a -> new DecompileOutput(
									a.deriveMavenMixHash(dirs.decomps(this.project), this.getInputHash(), Artifact.Type.SOURCES),
									a.deriveMavenMixHash(dirs.decomps(this.project), this.getInputHash())
							));
				}
			}
		};
		populate.accept(AmalgDirs.ROOT_PROJECT, this.inputLocal);
		populate.accept(AmalgDirs.GLOBAL, this.inputGlobal);

		if(isOutdated) {
			TaskContainer tasks = this.project.getTasks();
			GenerateSourcesTask task = tasks.create(this.optionalTask == null ? "decompile" + TASK_NAME_C++ : this.optionalTask,
					GenerateSourcesTask.class,
					this.decompiler);
			task.getDecompilerClasspath()
					.from(

					);

			task.getJavadocs().set(this.javadocs);
			List<GenerateSourcesTask.DecompileEntry> entries = new ArrayList<>();
			this.artifactMap.forEach((input, output) -> {
				if(output.sources.file.exists()) { // if already decompiled, just add to classpath
					task.getClasspath().from(AmalgIO.from(input.file));
				} else {
					GenerateSourcesTask.DecompileEntry entry = new GenerateSourcesTask.DecompileEntry();
					entry.linemappedFile = AmalgIO.from(output.linemapped.file);
					entry.outputFile = AmalgIO.from(output.sources.file);
					entry.inputFile = AmalgIO.from(input.file);
					entries.add(entry);
				}
			});
			task.getTasks().set(entries);
			for(Object o : this.classpath) {
				task.getClasspath().from(this.artifacts(o)
						.stream()
						.filter(a -> a.type != Artifact.Type.SOURCES)
						.map(a -> a.file)
						.map(AmalgIO::from)
						.toArray());
			}

			for(Object o : this.decompilerClasspath) {
				task.getDecompilerClasspath().from(this.artifacts(o)
						.stream()
						.filter(a -> a.type != Artifact.Type.SOURCES)
						.map(a -> a.file)
						.map(AmalgIO::from)
						.toArray());
			}

			if(this.optionalTask == null) {
				task.run();
				task.setEnabled(false);
			} else {
				task.appendParallelSafeAction(t -> {
					try {
						this.writeHash();
					} catch(IOException e) {
						throw FPInternal.rethrow(e);
					}
				});
				throw new Uncached(this.artifactMap.keySet());
			}
		} else {
			if(this.optionalTask != null) {
				TaskContainer tasks = this.project.getTasks();
				tasks.create(this.optionalTask).doFirst(t -> {
					this.logger.lifecycle("A cached decompilation already exists for this artifact, running this task now does nothing!");
				});
			}
		}
		return this.artifactMap.values()
				       .stream()
				       .flatMap(d -> Stream.of(d.sources, d.linemapped))
				       .collect(Collectors.toSet());
	}

	protected byte[] getInputHash() {
		if(this.inputHash == null) {
			Hasher hasher = HASHING.newHasher();
			for(GenerateSourcesTask.JavadocEntry javadoc : this.javadocs) {
				hasher.putString(javadoc.to, StandardCharsets.UTF_8);
				AmalgIO.hash(hasher, javadoc.mappings);
			}
			hasher.putString(this.decompiler.name, StandardCharsets.UTF_8);
			for(Object o : this.decompilerClasspath) {
				hashDep(hasher, o);
			}
			this.inputHash = hasher.hash().asBytes();
		}
		return this.inputHash;
	}

	record DecompileOutput(Artifact sources, Artifact linemapped) {}

	public class SingleDecompileDependency extends AmalgamationDependency {
		public final Object dependency;
		public final Set<Artifact> inputs;

		public SingleDecompileDependency(Object dependency) {
			super(DecompileDependency.this.project);
			this.dependency = dependency;
			this.inputs = this.artifacts(dependency);
		}

		@Override
		protected Set<Artifact> resolveArtifacts() throws IOException {
			DecompileDependency.this.resolveArtifacts();
			return this.inputs.stream()
					.map(DecompileDependency.this.artifactMap::get)
					.flatMap(d -> Stream.of(d.sources, d.linemapped))
					.collect(Collectors.toSet());
		}
	}
}
