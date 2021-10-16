package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import com.github.javaparser.ast.CompilationUnit;
import com.google.common.hash.Hasher;
import org.objectweb.asm.tree.ClassNode;

public interface Transformer {
	ClassNode apply(ClassNode node);

	void applyJava(CompilationUnit unit);

	/**
	 * hash the inputs to this transformer, if the hash changes, the jar changes
	 */
	void hash(Hasher hasher);

	default boolean processes(String path) {
		return path.endsWith(".class");
	}

	default int writerFlags() {
		return 0;
	}
}
