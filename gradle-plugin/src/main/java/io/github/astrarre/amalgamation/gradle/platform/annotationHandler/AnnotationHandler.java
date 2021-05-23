package io.github.astrarre.amalgamation.gradle.platform.annotationHandler;

import java.util.List;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;

public interface AnnotationHandler {
	@Nullable
	List<String> expand(AnnotationNode node);

	@Nullable
	AnnotationNode condense(PlatformId names);
}
