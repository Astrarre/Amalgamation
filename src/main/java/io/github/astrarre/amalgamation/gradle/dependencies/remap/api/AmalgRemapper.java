package io.github.astrarre.amalgamation.gradle.dependencies.remap.api;

import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.ZipOutput;
import org.objectweb.asm.commons.Remapper;

public interface AmalgRemapper {
	/**
	 * @return true if this remapper requires visiting the classpath
	 */
	boolean requiresClasspath();

	/**
	 * @return true if this remapper requires a post-emission stage {@link ZipRemapper#acceptPost(ZipOutput)}
	 */
	boolean hasPostStage();

	void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper);

	ZipRemapper createNew();

	void hash(Hasher hasher);

	record Combined(List<AmalgRemapper> remappers) implements AmalgRemapper {
		@Override
		public boolean requiresClasspath() {
			return this.remappers.stream().anyMatch(AmalgRemapper::requiresClasspath);
		}

		@Override
		public boolean hasPostStage() {
			return this.remappers.stream().anyMatch(AmalgRemapper::hasPostStage);
		}

		@Override
		public void acceptMappings(List<Mappings.Namespaced> list, Remapper r) {
			for(AmalgRemapper remapper : this.remappers) {
				remapper.acceptMappings(list, r);
			}
		}

		@Override
		public ZipRemapper createNew() {
			return new ZipRemapper.Combined(this.remappers.stream().map(AmalgRemapper::createNew).toList());
		}

		@Override
		public void hash(Hasher hasher) {
			for(AmalgRemapper remapper : this.remappers) {
				remapper.hash(hasher);
			}
		}
	}
}
