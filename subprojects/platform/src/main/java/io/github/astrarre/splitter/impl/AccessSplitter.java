package io.github.astrarre.splitter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.github.astrarre.Classes;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.util.SplitterUtil;
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
		for (AnnotationNode annotation : input.visibleAnnotations) {
			if (Classes.ACCESS_DESC.equals(annotation.desc)) {
				List<AnnotationNode> platforms = AsmUtil.get(annotation, "platforms", Collections.emptyList());
				if (SplitterUtil.matches(platforms, forPlatform)) {
					target.access = getAccess(annotation);
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
		return false;
	}
}
