package io.github.astrarre.amalgamation.gradle.platform.api.annotation;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;

public interface AnnotationHandler {
	@Nullable PlatformId parseClassPlatforms(AnnotationNode node);

	@Nullable AnnotationNode createClassPlatformAnnotation(PlatformId names);

	@Nullable Platformed<String> parseInterfacePlatforms(AnnotationNode node);

	@Nullable AnnotationNode createInterfacePlatformAnnotation(Platformed<String> type);

	@Nullable Platformed<String> parseSuperclassPlatforms(AnnotationNode node);

	@Nullable AnnotationNode createSuperclassPlatformAnnotation(Platformed<String> type);

	@Nullable PlatformId parseMethodPlatforms(AnnotationNode node);

	@Nullable AnnotationNode createMethodPlatformAnnotation(PlatformId id);

	@Nullable PlatformId parseFieldPlatforms(AnnotationNode node);

	@Nullable AnnotationNode createFieldPlatformAnnotation(PlatformId id);

	@Nullable Platformed<Integer> parseAccessPlatforms(AnnotationNode node);

	@Nullable AnnotationNode createAccessAnnotation(PlatformId id, int access);
}
