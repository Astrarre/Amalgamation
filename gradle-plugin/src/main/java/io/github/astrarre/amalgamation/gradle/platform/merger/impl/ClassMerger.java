package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class ClassMerger extends Merger {
	public ClassMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			AnnotationHandler handler) {
		Set<PlatformId> computed = new HashSet<>();
		// todo this needs to be taken out, do after refactater
		for (RawPlatformClass input : inputs) {
			ClassNode c = input.val;
			boolean visited = false;
			if(c.invisibleAnnotations != null) {
				for (AnnotationNode annotation : c.invisibleAnnotations) {
					PlatformId type = handler.parseClassPlatforms(annotation);
					if(type != null) {
						visited = true;
						computed.add(type);
					}
				}
			}

			if(!visited) {
				computed.add(input.id);
			}
		}

		for (PlatformId ids : computed) {
			if(target.invisibleAnnotations == null) target.invisibleAnnotations = new ArrayList<>();
			target.invisibleAnnotations.add(handler.createClassPlatformAnnotation(ids));
		}
	}
}
