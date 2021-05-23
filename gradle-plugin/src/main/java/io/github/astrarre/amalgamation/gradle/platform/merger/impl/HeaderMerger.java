package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class HeaderMerger extends Merger {
	public HeaderMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			List<AnnotationHandler> annotationHandlers) {
		ClassNode copyTo = inputs.get(0).val;
		target.name = copyTo.name;
		List<AnnotationNode> visible = new ArrayList<>();
		for (RawPlatformClass input : inputs) {
			ClassNode node = input.val;
			if(node.visibleAnnotations != null) {
				visible.addAll(node.visibleAnnotations);
			}
		}
		target.visibleAnnotations = visible;

		int maxVersion = 0;
		for (RawPlatformClass input : inputs) {
			if(input.val.version > maxVersion) {
				maxVersion = input.val.version;
			}
		}
		target.version = maxVersion;
	}
}
