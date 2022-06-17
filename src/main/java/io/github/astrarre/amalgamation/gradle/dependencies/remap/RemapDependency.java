package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.AmalgamationDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.util.BasicRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import io.github.astrarre.amalgamation.gradle.utils.zip.ZipIO;
import it.unimi.dsi.fastutil.Pair;
import org.gradle.api.Project;
import sleep.Sleep;

public class RemapDependency extends CachedDependency {
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
	
	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws Exception {
		logger.lifecycle("remember to remove the Thread.sleep u used to attach this debugger");
		logger.lifecycle("good luck!");
		
		AmalgamationRemapper remapper = this.config.getRemapper();
		List<AmalgamationRemapper> remappers = this.config.remappers;
		Set<Artifact> classpath = new HashSet<>();
		if(remapper.requiresClasspath()) {
			for(Object o : this.config.getClasspath()) {
				Set<Artifact> artifacts = this.artifacts(o);
				classpath.addAll(artifacts);
			}
		}
		
		boolean outdatedOverwrite = false;
		Map<Artifact, Artifact> cache = new HashMap<>();
		List<Pair<Artifact, Artifact>> remaps = new ArrayList<>();
		for(Single local : Iterables.concat(this.config.inputsLocal, this.config.inputsGlobal)) {
			for(Artifact artifact : this.artifacts(local.dependency)) {
				if(!cache.containsKey(artifact)) {
					Artifact output = this.transform(artifact, local.dirs);
					if(Files.exists(output.file)) {
						if(remapper.requiresClasspath()) {
							classpath.add(artifact);
						}
					} else {
						remaps.add(Pair.of(artifact, output));
						outdatedOverwrite = true;
					}
					cache.put(artifact, output);
				}
			}
		}
		
		if(isOutdated || outdatedOverwrite) {
			if(!isOutdated) {
				System.err.println("[Warning] Some remapped files were deleted, forcing remap to restore them!");
			}
			List<Mappings.Namespaced> mappings = new ArrayList<>();
			for(MappingTarget mapping : this.config.getMappings()) {
				mappings.add(mapping.read());
			}
			remapper.acceptMappings(mappings, new BasicRemapper(mappings));
			
			if(remapper.requiresClasspath()) {
				remapper.acceptClasspath(classpath);
				for(Artifact artifact : classpath) {
					FSRef src = ZipIO.readZip(artifact.file);
					AmalgamationRemapper.RemapTask task = remapper.classpathTask(artifact, src);
					// todo improve RemapTask can state whether it needs to walk through every file or not
					src.walk().filter(Files::isRegularFile).forEach(UCons.of(path -> task.acceptFile(src, path, null, false)));
				}
			}
			
			List<AmalgamationRemapper> otherRemappers = List.copyOf(remappers);
			remapper.acceptRemap(otherRemappers, remaps);
			List<FSRef> writes = new ArrayList<>();
			for(Pair<Artifact, Artifact> remap : remaps) {
				Artifact from = remap.first(), to = remap.second();
				FSRef src = ZipIO.readZip(from.file), dst = ZipIO.createZip(to.file);
				AmalgamationRemapper.RemapTarget target = new AmalgamationRemapper.RemapTarget(from, src, to, dst);
				AmalgamationRemapper.RemapTask task = remapper.createTask(target);
				long readStage = System.currentTimeMillis();
				
				//Remap Load Stage Took 4620ms
				//Remap Write Stage Took 4740ms
				//Unable to locate fabric.mod.json, defaulting to extension detection (.aw, .accesswidener, .accessWidener)
				//Remap Load Stage Took 5185ms
				//Remap Write Stage Took 490ms
				//src.walk(ForkJoinPool.commonPool(), UCons.of(path -> {
				//	if(!task.acceptFile(src, path, dst, false)) {
				//		Path out = dst.getPath(path.toString());
				//		AmalgIO.createParent(out);
				//		Files.copy(path, out);
				//	}
				//}));
				
				src.walk().filter(Files::isRegularFile).forEach(UCons.of(path -> {
					if(!task.acceptFile(src, path, dst, false)) {
						Path out = dst.getPath(path.toString());
						AmalgIO.createParent(out);
						Files.copy(path, out);
					}
				}));
				logger.lifecycle("Remap Load Stage Took " + (System.currentTimeMillis() - readStage) + "ms");
				writes.add(dst);
			}
			long writeStage = System.currentTimeMillis();
			remapper.write();
			for(FSRef write : writes) {
				write.flush();
			}
			logger.lifecycle("Remap Write Stage Took " + (System.currentTimeMillis() - writeStage) + "ms");
		}
		
		return Set.copyOf(cache.values());
	}
	
	AmalgDirs getDir() {
		return this.config.inputsLocal.isEmpty() ? AmalgDirs.GLOBAL : AmalgDirs.ROOT_PROJECT;
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
	
	public Single createDep(Object dep, AmalgDirs dirs) {
		return new Single(dep, dirs, false);
	}
}
