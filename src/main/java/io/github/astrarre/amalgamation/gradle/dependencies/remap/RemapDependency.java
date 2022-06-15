package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
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
import it.unimi.dsi.fastutil.Pair;
import net.devtech.filepipeline.api.VirtualPath;
import org.gradle.api.Project;

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
	protected VirtualPath evaluatePath(byte[] hash) throws IOException {
		return this.getDir().remaps(this.project).getDir(AmalgIO.b64(hash));
	}
	
	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		AmalgamationRemapper remapper = null;
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
					if(output.file.exists()) {
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
			if(outdatedOverwrite) {
				System.err.println("[Warning] Some remapped files were deleted, forcing remap to restore them!");
			}
			List<Mappings.Namespaced> mappings = new ArrayList<>();
			for(MappingTarget mapping : this.config.getMappings()) {
				mappings.add(mapping.read());
			}
			remapper.acceptMappings(mappings, new BasicRemapper(mappings));
			remapper.acceptClasspath(classpath);
			remapper.acceptRemaps(remaps);
			remapper.write();
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
