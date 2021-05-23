package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodSplitter extends Splitter {
	public MethodSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		for (MethodNode method : input.methods) {
			if (method.invisibleAnnotations == null) {
				target.methods.add(method);
				continue;
			}

			if (!MergeUtil.matches(method.invisibleAnnotations, forPlatform, annotationHandlers)) {
				continue;
			}

			method = copy(method);
			method.invisibleAnnotations = MergeUtil.stripAnnotations(method.invisibleAnnotations, forPlatform, annotationHandlers);
			for (AnnotationNode annotation : method.invisibleAnnotations) {
				if (Constants.DISPLACE_DESC.equals(annotation.desc)) {
					method.name = MergeUtil.get(annotation, "value", method.name);
				}
			}
			target.methods.add(method);
		}
		return false;
	}

	public static MethodNode copy(MethodNode node) {
		MethodNode n[] = {null};
		ClassVisitor visitor = new ClassVisitor(ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return n[0] = new MethodNode(access, name, descriptor, signature, exceptions);
			}
		};
		node.accept(visitor);
		return n[0];
	}
}
