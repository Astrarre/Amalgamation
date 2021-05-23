package io.github.astrarre.amalgamation.gradle.merger.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.merger.Merger;
import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.merger.api.Platformed;
import io.github.astrarre.amalgamation.gradle.merger.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class SuperclassMerger extends Merger {
	public SuperclassMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs, ClassNode target, Map<String, List<String>> platformCombinations) {
		Map<String, List<Platformed<String>>> supers = new HashMap<>();
		for (RawPlatformClass info : inputs) {
			if(info.val.invisibleAnnotations != null) {
				for (AnnotationNode annotation : info.val.invisibleAnnotations) {
					if (Constants.PARENT_DESC.equals(annotation.desc)) {
						List<AnnotationNode> platforms = MergeUtil.get(annotation, "platforms", Collections.emptyList());
						for (PlatformId platform : Platformed.getPlatforms(platforms, info.id)) {
							Type parent = MergeUtil.get(annotation, "parent", Constants.OBJECT_TYPE);
							String name = parent.getInternalName();
							supers.computeIfAbsent(name, s -> new ArrayList<>()).add(new Platformed<>(platform, name));
						}
					}
				}
			}
			supers.computeIfAbsent(info.val.superName, s -> new ArrayList<>()).add(new Platformed<>(info.id, info.val.superName));
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
				AnnotationVisitor n = target.visitAnnotation(Constants.PARENT_DESC, true);
				AnnotationVisitor visitor = n.visitArray("platform");
				for (Platformed<String> info : i) {
					visitor.visit("platform", info.id.createAnnotation());
				}

				n.visit("parent", Type.getObjectType(s));
			});
		}
	}
}
