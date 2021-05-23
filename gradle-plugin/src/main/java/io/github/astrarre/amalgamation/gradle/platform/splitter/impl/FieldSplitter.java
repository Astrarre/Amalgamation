package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class FieldSplitter extends Splitter {
	public FieldSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		for (FieldNode field : input.fields) {
			if (field.invisibleAnnotations == null) {
				target.fields.add(field);
				continue;
			}

			if (!MergeUtil.matches(field.invisibleAnnotations, forPlatform, annotationHandlers)) {
				continue;
			}

			field = copy(field);
			field.invisibleAnnotations = MergeUtil.stripAnnotations(field.invisibleAnnotations, forPlatform, annotationHandlers);
			for (AnnotationNode annotation : field.invisibleAnnotations) {
				if (Constants.DISPLACE_DESC.equals(annotation.desc)) {
					field.name = MergeUtil.get(annotation, "value", field.name);
				}
			}
			target.fields.add(field);
		}
		return false;
	}

	public static FieldNode copy(FieldNode node) {
		FieldNode n[] = {null};
		ClassVisitor visitor = new ClassVisitor(ASM9) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				return n[0] = new FieldNode(access, name, descriptor, signature, value);
			}
		};
		node.accept(visitor);
		return n[0];
	}
}
