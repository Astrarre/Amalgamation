package io.github.astrarre.amalgamation.gradle.dependencies.remap.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import it.unimi.dsi.fastutil.Pair;
import org.objectweb.asm.commons.Remapper;

public interface AmalgamationRemapper {
	void hash(Hasher hasher);
	
	default boolean requiresClasspath() {return false;}
	
	/**
	 * May be called multiple times
	 */
	default void acceptClasspath(Set<Artifact> classpath) {}
	
	default void acceptRemap(List<AmalgamationRemapper> remappers, List<Pair<Artifact, Artifact>> fromTos) throws Exception {}
	
	RemapTask createTask(RemapTarget target);
	
	default RemapTask classpathTask(Artifact from, FSRef fromFs) {
		return (srcFs, srcPath, dstFs, handled) -> false;
	}
	
	record RemapTarget(Artifact from, FSRef fromFs, Artifact to, FSRef toFs) {}
	
	interface RemapTask {
		/**
		 * @param handled if the file was already remapped by a prior task
		 * @return true if the given file was remapped and shouldn't be copied
		 */
		boolean acceptFile(FSRef srcFs, Path srcPath, FSRef dstFs, boolean handled) throws IOException;
	}
	
	default void write() {}
	
	void acceptMappings(List<Mappings.Namespaced> mappings, Remapper remapper);
	
	record Combined(List<AmalgamationRemapper> remappers) implements AmalgamationRemapper {
		@Override
		public void hash(Hasher hasher) {
			for(AmalgamationRemapper remapper : this.remappers) {
				remapper.hash(hasher);
			}
		}
		
		@Override
		public void acceptRemap(
				List<AmalgamationRemapper> remappers, List<Pair<Artifact, Artifact>> fromTos) throws Exception {
			
			for(AmalgamationRemapper remapper : this.remappers) {
				remapper.acceptRemap(remappers, fromTos);
			}
		}
		
		@Override
		public RemapTask createTask(RemapTarget target) {
			return this.extracted(r -> r.createTask(target));
		}
		
		private RemapTask extracted(Function<AmalgamationRemapper, RemapTask> create) {
			List<RemapTask> tasks = new ArrayList<>(this.remappers.size());
			for(AmalgamationRemapper remapper : this.remappers) {
				tasks.add(create.apply(remapper));
			}
			
			return (srcFs, srcPath, dstFs, handled) -> {
				for(RemapTask task : tasks) {
					handled |= task.acceptFile(srcFs, srcPath, dstFs, handled);
				}
				return handled;
			};
		}
		
		@Override
		public RemapTask classpathTask(Artifact from, FSRef fromFs) {
			return this.extracted(r -> r.classpathTask(from, fromFs));
		}
		
		@Override
		public void acceptMappings(List<Mappings.Namespaced> mappings, Remapper remapper) {
			for(AmalgamationRemapper amalgamationRemapper : this.remappers) {
				amalgamationRemapper.acceptMappings(mappings, remapper);
			}
		}
		
		@Override
		public boolean requiresClasspath() {
			for(AmalgamationRemapper remapper : this.remappers) {
				if(remapper.requiresClasspath()) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public void acceptClasspath(Set<Artifact> classpath) {
			for(AmalgamationRemapper remapper : this.remappers) {
				remapper.acceptClasspath(classpath);
			}
		}
		
		@Override
		public void write() {
			for(AmalgamationRemapper remapper : this.remappers) {
				remapper.write();
			}
		}
	}
}
