package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class HeaderSplitter extends Splitter {
	public HeaderSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler annotationHandlers) {
		target.name = input.name;
		target.version = input.version;
		target.visibleAnnotations = input.visibleAnnotations;
		// todo handle versions @Version(...)
		return false;
	}
}
