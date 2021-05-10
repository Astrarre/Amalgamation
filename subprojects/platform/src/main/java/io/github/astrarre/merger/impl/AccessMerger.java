package io.github.astrarre.merger.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.Classes;
import io.github.astrarre.merger.Merger;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.f2bb.amalgamation.Access;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class AccessMerger extends Merger implements Opcodes {
	public static final String ACCESS = Type.getDescriptor(Access.class);

	public AccessMerger(Map<String, ?> properties) {
		super(properties);
	}

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
	public void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations) {
		MultiValuedMap<Integer, PlatformId> accessFlags = new ArrayListValuedHashMap<>();
		for (Platformed<ClassNode> input : inputs) {
			for (Platformed<ClassNode> platformed : input.split(AsmUtil.get(AsmUtil.withDesc(input.val.invisibleAnnotations,
					Classes.ACCESS_DESC,
					node -> true), "platforms", Collections.emptyList()))) {
				accessFlags.put(platformed.val.access, platformed.id);
			}
		}

		int widest = getWidest(accessFlags);
		accessFlags.remove(widest);
		target.access = widest;

		if (!accessFlags.isEmpty()) {
			if (target.invisibleAnnotations == null) {
				target.invisibleAnnotations = new ArrayList<>();
			}
			target.invisibleAnnotations.addAll(visit(accessFlags, true));
		}
	}

	public static int getWidest(MultiValuedMap<Integer, PlatformId> accessFlags) {
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

	public static List<AnnotationNode> visit(MultiValuedMap<Integer, PlatformId> accessFlags, boolean isClass) {
		List<AnnotationNode> node = new ArrayList<>();
		accessFlags.asMap().forEach((access, info) -> {
			AnnotationNode accessAnnotation = new AnnotationNode(ACCESS);
			AnnotationVisitor platforms = accessAnnotation.visitArray("platforms");
			for (PlatformId classInfo : info) {
				platforms.visit("platforms", classInfo.createAnnotation());
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

			if(isClass) {
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
			}

			if (Modifier.isFinal(access)) {
				flags.visit("flags", "final");
			}

			if (Modifier.isStatic(access)) {
				flags.visit("flags", "static");
			}

			if (Modifier.isSynchronized(access) && !isClass) {
				flags.visit("flags", "synchronized");
			}

			if (Modifier.isVolatile(access) && !isClass) {
				flags.visit("flags", "volatile");
			}

			if (Modifier.isTransient(access) && !isClass) {
				flags.visit("flags", "transient");
			}

			if (Modifier.isNative(access) && !isClass) {
				flags.visit("flags", "native");
			}

			flags.visitEnd();
			accessAnnotation.visitEnd();
			node.add(accessAnnotation);
		});
		return node;
	}
}
