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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodSplitter extends Splitter {
	public MethodSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		for (MethodNode method : input.methods) {
			if (method.invisibleAnnotations == null) {
				target.methods.add(method);
				continue;
			}

			if (!SplitterUtil.matches(method.invisibleAnnotations, forPlatform)) {
				continue;
			}

			method = copy(method);
			method.invisibleAnnotations = SplitterUtil.stripAnnotations(method.invisibleAnnotations, forPlatform);
			for (AnnotationNode annotation : method.invisibleAnnotations) {
				if (Classes.DISPLACE_DESC.equals(annotation.desc)) {
					method.name = AsmUtil.get(annotation, "value", method.name);
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
