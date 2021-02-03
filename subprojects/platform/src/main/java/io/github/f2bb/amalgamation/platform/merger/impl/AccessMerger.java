/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.f2bb.amalgamation.platform.merger.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.f2bb.amalgamation.Access;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class AccessMerger implements Merger {
	public static final String ACCESS = Type.getDescriptor(Access.class);

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

	@Override
	public void merge(MergerContext mergerContext) {
		Map<Integer, List<ClassInfo>> accessFlags = new HashMap<>();
		for (ClassInfo info : mergerContext.getInfos()) {
			accessFlags.computeIfAbsent(info.node.access, s -> new ArrayList<>()).add(info);
		}

		int widest = getWidest(accessFlags);
		accessFlags.remove(widest);
		mergerContext.getNode().access = widest;

		if (!accessFlags.isEmpty()) {
			if (mergerContext.getNode().visibleAnnotations == null) {
				mergerContext.getNode().visibleAnnotations = new ArrayList<>();
			}
			mergerContext.getNode().visibleAnnotations.addAll(visit(accessFlags));
		}
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		Iterator<AnnotationNode> iterator = in.visibleAnnotations.iterator();
		while (iterator.hasNext()) {
			AnnotationNode annotation = iterator.next();
			if (ACCESS.equals(annotation.desc)) {
				List<Object> values = annotation.values;
				if (SplitterUtil.matches((List<AnnotationNode>) values.get(values.indexOf("platforms") + 1), available)) {
					in.access = getAccess(annotation);
				}
				iterator.remove();
			}
		}
		return false;
	}

	public static int getWidest(Map<Integer, List<ClassInfo>> accessFlags) {
		int widest = 0;
		for (int access : accessFlags.keySet()) {
			if (Modifier.isPublic(access)) {
				widest = access;
			} else if (Modifier.isProtected(access) && !Modifier.isPublic(widest)) {
				widest = access;
			} else if (widest == 0) {
				widest = access;
			}
		}
		return widest;
	}

	public static List<AnnotationNode> visit(Map<Integer, List<ClassInfo>> accessFlags) {
		List<AnnotationNode> node = new ArrayList<>();
		accessFlags.forEach((access, info) -> {
			AnnotationNode accessAnnotation = new AnnotationNode(ACCESS);
			AnnotationVisitor platforms = accessAnnotation.visitArray("platforms");
			for (ClassInfo classInfo : info) {
				platforms.visit("platforms", classInfo.createPlatformAnnotation());
			}
			platforms.visitEnd();

			AnnotationVisitor flags = accessAnnotation.visitArray("flags");
			if (Modifier.isPublic(access)) {
				flags.visit("flags", "public");
			} else if (Modifier.isProtected(access)) {
				flags.visit("flags", "protected");
			} else if (Modifier.isPrivate(access)) {
				flags.visit("flags", "private");
			} else {
				flags.visit("flags", "package-private");
			}

			if (Modifier.isInterface(access)) {
				flags.visit("flags", "interface");
			} else if (Modifier.isAbstract(access)) {
				flags.visit("flags", "abstract");
				flags.visit("flags", "class");
			} else if ((ACC_ENUM & access) != 0) {
				flags.visit("flags", "enum");
			} else if ((ACC_ANNOTATION & access) != 0) {
				flags.visit("flags", "@interface");
			} else {
				flags.visit("flags", "class");
			}

			if (Modifier.isFinal(access)) {
				flags.visit("flags", "final");
			}

			if (Modifier.isStatic(access)) {
				flags.visit("flags", "static");
			}

			if (Modifier.isSynchronized(access)) {
				flags.visit("flags", "synchronized");
			}

			if (Modifier.isVolatile(access)) {
				flags.visit("flags", "volatile");
			}

			if (Modifier.isTransient(access)) {
				flags.visit("flags", "transient");
			}

			if (Modifier.isNative(access)) {
				flags.visit("flags", "native");
			}

			flags.visitEnd();
			accessAnnotation.visitEnd();
			node.add(accessAnnotation);
		});
		return node;
	}
}
