package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import com.google.common.hash.Hasher;
import org.objectweb.asm.tree.ClassNode;

public interface Transformer {
	void apply(ClassNode node);

	/**
	 * hash the inputs to this transformer, if the hash changes, the jar changes
	 */
	void hash(Hasher hasher);

	default boolean processes(String path) {
		return path.endsWith(".class");
	}

	default byte[] processResource(String path, byte[] input) {
		return input;
	}

	default int writerFlags() {
		return 0;
	}
}
