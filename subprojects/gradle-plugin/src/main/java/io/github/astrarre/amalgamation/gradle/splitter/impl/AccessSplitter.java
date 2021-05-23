package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.merger.util.AsmUtil;
import io.github.astrarre.amalgamation.gradle.splitter.util.SplitterUtil;
import io.github.astrarre.amalgamation.gradle.utils.Classes;
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
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		if(input.invisibleAnnotations == null) {
			target.access = input.access;
			return false;
		}
		boolean found = false;
		for (AnnotationNode annotation : input.invisibleAnnotations) {
			if (Classes.ACCESS_DESC.equals(annotation.desc)) {
				List<AnnotationNode> platforms = AsmUtil.get(annotation, "platforms", Collections.emptyList());
				if (SplitterUtil.matches(platforms, forPlatform)) {
					target.access = getAccess(annotation);
					found = true;
					List<AnnotationNode> stripped = SplitterUtil.stripAnnotations(platforms, forPlatform);
					if(!stripped.isEmpty()) {
						AnnotationNode access = new AnnotationNode(Classes.ACCESS_DESC);
						access.values.add("flags");
						access.values.add(AsmUtil.get(annotation, "flags", Collections.emptyList()));
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
