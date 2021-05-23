package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import org.objectweb.asm.tree.ClassNode;

public class ClassMerger extends Merger {
	public ClassMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			List<AnnotationHandler> annotationHandlers) {
		Set<PlatformId> computed = new HashSet<>();
		for (RawPlatformClass input : inputs) {
			for (Platformed<ClassNode> platformed : input.split(annotationHandlers, n -> n.invisibleAnnotations)) {
				computed.add(platformed.id);
			}
		}

		for (PlatformId ids : computed) { // todo reduce
			if(target.invisibleAnnotations == null) target.invisibleAnnotations = new ArrayList<>();
			target.invisibleAnnotations.add(ids.createAnnotation(annotationHandlers));
		}
	}
}
