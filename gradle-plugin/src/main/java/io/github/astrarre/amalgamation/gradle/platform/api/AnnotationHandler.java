package io.github.astrarre.amalgamation.gradle.platform.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;

public interface AnnotationHandler {
	@Nullable
	List<String> expand(AnnotationNode node);

	@Nullable
	AnnotationNode condense(PlatformId names);
}
