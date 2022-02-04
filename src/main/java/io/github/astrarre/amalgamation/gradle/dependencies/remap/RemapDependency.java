package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.AmalgamationDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.ZipRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.util.BasicRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.util.FinishedZipFilter;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.zip.ZipFilter;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;

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

	public Single createDep(Object dep, AmalgDirs dirs) {
		return new Single(dep, dirs, false);
	}

	AmalgDirs getDir() {
		return this.config.inputsLocal.isEmpty() ? AmalgDirs.GLOBAL : AmalgDirs.ROOT_PROJECT;
	}

	// todo undo zip-io, it's not worth the speed I think
	// well, maybe, I think there is a way around this
	// in the "add" stage, we need a different kind of processable
	// one that can define visitor tasks like there is now
	// but also have a "post-process" which runs after everything
	// and holds up any child visitors

	// make a wrapper around Path that stores the uncompressed version of the data and emulates all the apis
	// maybe an actual Path should be optionally provided so you can use the Path api but idk
	// make it easier to define "no output, just pipe to next"
	// and stuff like that
	// remove the "post visit entry" and whatever stages
	// they're useless anyways, you just need to store the "post visit" things yourself
	// and iterate through them yourself in a post/finisher stage

	// wrapper should be able to store custom data (like ClassNode)
	// and visitor system should be able to pass custom visitors (like ClassVisitor)

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		Map<Artifact, Artifact> map = new ConcurrentHashMap<>();
		if(isOutdated) {
			// basically just slap everything in and then zip filter if it exists
			Map<OutputTag, ZipRemapper> inputRemapperCache = new ConcurrentHashMap<>();

			AmalgRemapper amalgRemapper = this.config.getRemapper();

			if(amalgRemapper.requiresClasspath()) {
				for(Object o : this.config.getClasspath()) {
					this.apply(process, o, artifact -> OutputTag.INPUT);
				}
			}

			for(Single local : Iterables.concat(this.config.inputsLocal, this.config.inputsGlobal)) {
				UnaryOperator<Artifact> mapped = a -> {
					Artifact artifact = map.computeIfAbsent(a, ar -> this.transform(ar, local.dirs));
					local.artifacts.add(artifact);
					return artifact;
				};

				Set<Path> producedArtifacts = new HashSet<>();
				for(Artifact artifact : this.artifacts(local.dependency, false)) {
					if(Files.exists(mapped.apply(artifact).path)) {
						producedArtifacts.add(real(artifact.path));
					}
				}

				for(TaskTransform task : this.apply(process, local.dependency, mapped::apply)) {
					if(!amalgRemapper.requiresClasspath()) { // skip reading partial remap if classpath is not needed
						ZipFilter filter = FinishedZipFilter.createDefault(mapped);
						task.setZipFilter(o -> filter);
					}

					task.setPreEntryProcessor(tag -> {
						// if classpath is needed, but output already exists, then we can just input the input as classpath
						boolean exists = producedArtifacts.contains(real(tag.path));
						if(amalgRemapper.requiresClasspath() && exists) {
							ZipRemapper remapper = amalgRemapper.createNew();
							return buffer -> {
								remapper.visitEntry(buffer, true);
								return ProcessResult.HANDLED;
							};
						} else if(!exists) { // actual input
							ZipRemapper remapper;
							if(amalgRemapper.hasPostStage()) {
								remapper = inputRemapperCache.computeIfAbsent(tag, o -> amalgRemapper.createNew());
							} else {
								remapper = amalgRemapper.createNew();
							}
							return buffer -> {
								if(!remapper.visitEntry(buffer, false)) {
									buffer.copyToOutput();
								}
								return ProcessResult.HANDLED;
							};
						} else {
							throw new UnsupportedOperationException();
						}
					});
					
					if(amalgRemapper.hasPostStage()) {
						task.setFinalizingZipProcessor(o -> {
							var remap = inputRemapperCache.computeIfAbsent(o, tag -> amalgRemapper.createNew());
							return remap::acceptPost;
						});
					}
				}
			}

			List<Mappings.Namespaced> mappings = new ArrayList<>();
			for(MappingTarget mapping : this.config.getMappings()) {
				mappings.add(mapping.read());
			}
			this.config.getRemapper().acceptMappings(mappings, new BasicRemapper(mappings));
		} else {
			for(Single local : Iterables.concat(this.config.inputsLocal, this.config.inputsGlobal)) {
				UnaryOperator<Artifact> mapped = a -> {
					Artifact artifact = map.computeIfAbsent(a, ar -> this.transform(ar, local.dirs));
					local.artifacts.add(artifact);
					return artifact;
				};
				for(Artifact artifact : this.artifacts(local.dependency, true)) {
					mapped.apply(artifact);
				}
			}
			for(Artifact value : map.values()) {
				process.addProcessed(value);
			}
		}
	}

	private Artifact transform(Artifact artifact, AmalgDirs dirs) {
		return artifact.deriveMavenMixHash(dirs.remaps(project), config.getMappingsHash());
	}

	public class Single extends AmalgamationDependency {
		final Object dependency;
		final AmalgDirs dirs;
		final boolean classpath;
		Set<Artifact> artifacts = new HashSet<>();

		public Single(Object dependency, AmalgDirs dirs, boolean classpath) {
			super(RemapDependency.this.project);
			this.dependency = dependency;
			this.dirs = dirs;
			this.classpath = classpath;
		}

		@Override
		protected Set<Artifact> resolveArtifacts() throws IOException {
			RemapDependency.this.getArtifacts();
			return this.artifacts;
		}
	}

	static Path real(Path path) {
		try {
			return path.toRealPath();
		} catch(IOException e) {
			throw U.rethrow(e);
		}
	}
}
