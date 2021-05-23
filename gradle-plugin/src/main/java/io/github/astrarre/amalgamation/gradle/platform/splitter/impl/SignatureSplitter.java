package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class SignatureSplitter extends Splitter {
	public SignatureSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		target.signature = input.signature; // todo intelligent signature merging
		return false;
	}
}
