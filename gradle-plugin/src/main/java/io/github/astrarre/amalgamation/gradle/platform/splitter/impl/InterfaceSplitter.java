package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceSplitter extends Splitter {
	public InterfaceSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		target.interfaces = new ArrayList<>(input.interfaces);
		if (input.invisibleAnnotations != null) {
			for (AnnotationNode annotation : input.invisibleAnnotations) {
				if(Constants.INTERFACE_DESC.equals(annotation.desc)) {
					Type type = MergeUtil.get(annotation, "parent", Constants.OBJECT_TYPE);
					List<AnnotationNode> platforms = MergeUtil.get(annotation, "platforms", Collections.emptyList());
					if(!MergeUtil.matches(platforms, forPlatform, annotationHandlers)) {
						target.interfaces.remove(type.getInternalName());
					}
				}
			}
		}

		return false;
	}
}
