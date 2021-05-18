package io.github.astrarre.splitter.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import io.github.astrarre.Classes;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.util.SplitterUtil;
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
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		for (FieldNode field : input.fields) {
			if (field.invisibleAnnotations == null) {
				target.fields.add(field);
				continue;
			}

			if (!SplitterUtil.matches(field.invisibleAnnotations, forPlatform)) {
				continue;
			}

			field = copy(field);
			field.invisibleAnnotations = SplitterUtil.stripAnnotations(field.invisibleAnnotations, forPlatform);
			for (AnnotationNode annotation : field.invisibleAnnotations) {
				if (Classes.DISPLACE_DESC.equals(annotation.desc)) {
					field.name = AsmUtil.get(annotation, "value", field.name);
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
