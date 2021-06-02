package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
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
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler handler) {
		for (FieldNode field : input.fields) {
			boolean visited = false;
			FieldNode copy = copy(field);
			if (field.invisibleAnnotations != null) {
				for (AnnotationNode annotation : field.invisibleAnnotations) {
					PlatformId fieldPlatform = handler.parseFieldPlatforms(annotation);
					if(fieldPlatform != null) {
						if(fieldPlatform.names.containsAll(forPlatform.names)) {
							visited = true;
							List<String> newNames = new ArrayList<>(fieldPlatform.names);
							newNames.removeAll(forPlatform.names);
							if(!newNames.isEmpty()) {
								if(copy.invisibleAnnotations == null) copy.invisibleAnnotations = new ArrayList<>();
								copy.invisibleAnnotations.add(handler.createFieldPlatformAnnotation(new PlatformId(newNames)));
							}
						}
					}
				}
			}
			if(!visited) {
				target.fields.add(copy);
			}
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
