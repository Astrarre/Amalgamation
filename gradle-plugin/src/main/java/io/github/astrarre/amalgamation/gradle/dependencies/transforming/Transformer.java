package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import org.objectweb.asm.tree.ClassNode;

public interface Transformer {
	void apply(ClassNode node);

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
