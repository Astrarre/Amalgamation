package io.github.astrarre.amalgamation.gradle.dependencies.remap.api;

import java.util.List;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import it.unimi.dsi.fastutil.Pair;
import org.objectweb.asm.commons.Remapper;

public interface AmalgamationRemapper {
	void hash(Hasher hasher);
	
	default boolean requiresClasspath() {return false;}
	
	/**
	 * May be called multiple times
	 */
	default void acceptClasspath(Set<Artifact> classpath) {}
	
	void acceptRemaps(List<Pair<Artifact, Artifact>> fromTos) throws Exception;
	
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
		public void acceptRemaps(List<Pair<Artifact, Artifact>> fromTos) throws Exception {
			for(AmalgamationRemapper remapper : this.remappers) {
				remapper.acceptRemaps(fromTos);
			}
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
