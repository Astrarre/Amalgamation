package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.tree.ClassNode;

public class ClassSplitter extends Splitter {
	public ClassSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		if(input.invisibleAnnotations != null) {
			if(!MergeUtil.matches(input.invisibleAnnotations, forPlatform, annotationHandlers)) {
				return true;
			}
			target.invisibleAnnotations = MergeUtil.stripAnnotations(input.invisibleAnnotations, forPlatform, annotationHandlers);
		}
		return false;
	}
}
