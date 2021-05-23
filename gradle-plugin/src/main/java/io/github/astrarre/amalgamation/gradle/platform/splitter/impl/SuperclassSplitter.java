package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class SuperclassSplitter extends Splitter {
	public SuperclassSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		target.superName = input.superName; // todo parent annotation
		return false;
	}
}
