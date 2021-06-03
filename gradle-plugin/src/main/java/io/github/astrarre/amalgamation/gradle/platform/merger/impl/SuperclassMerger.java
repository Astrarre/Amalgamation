package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class SuperclassMerger extends Merger {
	public SuperclassMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs, ClassNode target, Map<String, List<String>> platformCombinations, AnnotationHandler handler) {
		Map<String, List<Platformed<String>>> supers = new HashMap<>();
		for (RawPlatformClass input : inputs) {
			if (input.val.invisibleAnnotations != null) {
				for (AnnotationNode annotation : input.val.invisibleAnnotations) {
					Platformed<String> type = handler.parseSuperclassPlatforms(annotation);
					if(type != null) {
						supers.computeIfAbsent(type.val, s -> new ArrayList<>()).add(new Platformed<>(type.id, type.val));
					}
				}
			}

			supers.computeIfAbsent(input.val.superName, s -> new ArrayList<>()).add(new Platformed<>(input.id, input.val.superName));
		}

		// most common super class, this gets priority and is what is shown in the source
		String mostCommon = null;
		int count = 0;
		for (String s : supers.keySet()) {
			int size = supers.get(s).size();
			if (size > count) {
				mostCommon = s;
				count = size;
			}
		}

		if (mostCommon == null && count == 0) {
			throw new IllegalStateException("no classes! " + supers);
		}

		target.superName = mostCommon;

		supers.remove(mostCommon);
		if (!supers.isEmpty()) {
			supers.forEach((s, i) -> {
				for (Platformed<String> platformed : i) {
					AnnotationNode node = handler.createSuperclassPlatformAnnotation(platformed);
					if(target.invisibleAnnotations == null) target.invisibleAnnotations = new ArrayList<>();
					target.invisibleAnnotations.add(node);
				}
			});
		}
	}
}
