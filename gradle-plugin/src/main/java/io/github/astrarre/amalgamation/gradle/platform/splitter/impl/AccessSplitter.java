package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
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
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler handler) {
		boolean found = false;
		if(target.invisibleAnnotations != null) {
			for (int i = target.invisibleAnnotations.size() - 1; i >= 0; i--) {
				AnnotationNode annotation = target.invisibleAnnotations.get(i);
				Platformed<Integer> access = handler.parseAccessPlatforms(annotation);
				if (access != null) {
					target.invisibleAnnotations.remove(i);
					if (access.id.names.containsAll(forPlatform.names)) {
						target.access = access.val;
						found = true;
						List<String> newNames = new ArrayList<>(access.id.names);
						newNames.removeAll(forPlatform.names);
						if (!newNames.isEmpty()) {
							target.invisibleAnnotations.add(handler.createAccessAnnotation(new PlatformId(newNames), access.val));
						}
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
