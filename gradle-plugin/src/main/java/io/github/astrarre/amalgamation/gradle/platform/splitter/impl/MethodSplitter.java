package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
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
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler handler) {
		for (MethodNode method : input.methods) {
			boolean visited = false;
			MethodNode copy = copy(method);
			if (method.invisibleAnnotations != null) {
				for (AnnotationNode annotation : method.invisibleAnnotations) {
					PlatformId methodPlatform = handler.parseMethodPlatforms(annotation);
					if(methodPlatform != null) {
						if(methodPlatform.names.containsAll(forPlatform.names)) {
							visited = true;
							List<String> newNames = new ArrayList<>(methodPlatform.names);
							newNames.removeAll(forPlatform.names);
							if(!newNames.isEmpty()) {
								if(copy.invisibleAnnotations == null) copy.invisibleAnnotations = new ArrayList<>();
								copy.invisibleAnnotations.add(handler.createMethodPlatformAnnotation(new PlatformId(newNames)));
							}
						}
					}
				}
			}
			if(!visited) {
				target.methods.add(copy);
			}
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
