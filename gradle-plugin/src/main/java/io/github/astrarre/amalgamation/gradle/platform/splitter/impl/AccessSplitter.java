package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class AccessSplitter extends Splitter {
	public static int getAccess(AnnotationNode annotation) {
		List<Object> values = annotation.values;
		List<String> vals = (List<String>) values.get(values.indexOf("flags") + 1);
		int access = 0;
		for (String val : vals) {
			switch (val) {
			case "public":
				access |= ACC_PUBLIC;
				break;
			case "protect":
				access |= ACC_PROTECTED;
				break;
			case "private":
				access |= ACC_PRIVATE;
				break;
			case "interface":
				access |= ACC_INTERFACE;
				break;
			case "abstract":
				access |= ACC_ABSTRACT;
				break;
			case "enum":
				access |= ACC_ENUM;
				break;
			case "@interface":
				access |= ACC_ANNOTATION;
				break;
			case "final":
				access |= ACC_FINAL;
				break;
			case "static":
				access |= ACC_STATIC;
				break;
			case "synchronized":
				access |= ACC_SYNCHRONIZED;
				break;
			case "volatile":
				access |= ACC_VOLATILE;
				break;
			case "transient":
				access |= ACC_TRANSIENT;
				break;
			case "native":
				access |= ACC_NATIVE;
				break;
			}
		}
		return access;
	}

	public AccessSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		if(input.invisibleAnnotations == null) {
			target.access = input.access;
			return false;
		}
		boolean found = false;
		for (AnnotationNode annotation : input.invisibleAnnotations) {
			if (Constants.ACCESS_DESC.equals(annotation.desc)) {
				List<AnnotationNode> platforms = MergeUtil.get(annotation, "platforms", Collections.emptyList());
				if (MergeUtil.matches(platforms, forPlatform, MergeUtil.ONLY_PLATFORM)) {
					target.access = getAccess(annotation);
					found = true;
					List<AnnotationNode> stripped = MergeUtil.stripAnnotations(platforms, forPlatform, MergeUtil.ONLY_PLATFORM);
					if(!stripped.isEmpty()) {
						AnnotationNode access = new AnnotationNode(Constants.ACCESS_DESC);
						access.values.add("flags");
						access.values.add(MergeUtil.get(annotation, "flags", Collections.emptyList()));
						access.values.add("platforms");
						access.values.add(stripped);
					}
				}
			}
		}

		if(!found) {
			target.access = input.access;
		}
		return false;
	}
}
