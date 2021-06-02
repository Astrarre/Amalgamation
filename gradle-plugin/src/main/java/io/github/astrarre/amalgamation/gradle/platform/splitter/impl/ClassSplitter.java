package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class ClassSplitter extends Splitter {
	public ClassSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler annotationHandlers) {
		if(input.invisibleAnnotations != null) { // todo take this out, not a splitter, make sure to adjust forPlatform
			if(!Platformed.matches(input.invisibleAnnotations, forPlatform, annotationHandlers)) {
				return true;
			}
			target.invisibleAnnotations = Platformed.stripAnnotations(input.invisibleAnnotations, forPlatform, annotationHandlers);
		}
		return false;
	}
}
