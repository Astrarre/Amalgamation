package io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper;

import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import net.devtech.zipio.processors.zip.PostZipProcessor;

public interface AmalgRemapper {
	void init(List<Mappings.Namespaced> tree);

	ZipEntryProcessor classpath();

	Remap remap();

	void hash(Hasher hasher);

	/**
	 * If false, assumes that this is a completely streaming process, and so the PostZipProcessor in Remap will not be used!
	 */
	default boolean needsClasspath() {
		return true;
	}

	interface Remap extends ZipEntryProcessor, PostZipProcessor {
	}

	default void close() {}
}
