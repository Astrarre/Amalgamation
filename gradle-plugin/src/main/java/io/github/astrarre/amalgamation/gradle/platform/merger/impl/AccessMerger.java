package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.api.Access;
import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
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


	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			List<AnnotationHandler> annotationHandlers) {
		MultiValuedMap<Integer, PlatformId> accessFlags = new ArrayListValuedHashMap<>();
		for (RawPlatformClass input : inputs) {
			for (Platformed<ClassNode> platformed : input.split(MergeUtil.ONLY_PLATFORM, MergeUtil.get(MergeUtil.withDesc(input.val.invisibleAnnotations,
					Constants.ACCESS_DESC,
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
				platforms.visit("platforms", classInfo.createAnnotation(MergeUtil.ONLY_PLATFORM));
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
