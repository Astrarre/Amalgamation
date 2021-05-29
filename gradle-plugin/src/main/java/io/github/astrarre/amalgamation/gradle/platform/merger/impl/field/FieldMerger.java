package io.github.astrarre.amalgamation.gradle.platform.merger.impl.field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class FieldMerger extends Merger {
	public FieldMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			List<AnnotationHandler> annotationHandlers) {
		Set<PlatformId> all = new HashSet<>();
		MultiValuedMap<FieldKey, PlatformId> fieldAgreementMap = new ArrayListValuedHashMap<>();
		for (RawPlatformClass input : inputs) {
			for (Platformed<FieldNode> platformed : input.split(annotationHandlers, c -> c.fields, (c, f) -> f.invisibleAnnotations)) {
				fieldAgreementMap.put(new FieldKey(platformed.val), platformed.id);
				all.add(platformed.id);
			}
		}

		fieldAgreementMap.asMap().forEach((key, ids) -> {
			FieldNode clone = new FieldNode(key.node.access, key.node.name, key.node.desc, key.node.signature, null);
			key.node.accept(new ClassVisitor(ASM9) {
				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
					return clone;
				}
			});

			int counter = 0;
			String current = key.node.name;
			while (this.hasField(target, current)) {
				current = key.node.name + counter;
				counter++;
			}

			if(counter != 0) {
				if(clone.invisibleAnnotations == null) clone.invisibleAnnotations = new ArrayList<>();
				clone.invisibleAnnotations.add(displace(key.node.name));
				clone.name = current;
			}

			if(ids.size() != all.size()) {
				if (clone.invisibleAnnotations == null) {
					clone.invisibleAnnotations = new ArrayList<>();
				}

				for (PlatformId classInfo : ids) {
					clone.invisibleAnnotations.add(classInfo.createAnnotation(annotationHandlers));
				}
			}

			target.fields.add(clone);
		});
	}

	private boolean hasField(ClassNode node, String name) {
		for (FieldNode field : node.fields) {
			if (field.name.equals(name)) {
				return true;
			}
		}

		return false;
	}

	public static AnnotationNode displace(String name) {
		AnnotationNode node = new AnnotationNode(Constants.DISPLACE_DESC);
		node.visit("value", name);
		node.visitEnd();
		return node;
	}
}