package io.github.astrarre.merger.impl.field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.Classes;
import io.github.astrarre.merger.Merger;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
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
	public void merge(List<Platformed<ClassNode>> inputs, ClassNode target, List<List<String>> platformCombinations) {
		Set<PlatformId> all = new HashSet<>();
		MultiValuedMap<FieldKey, PlatformId> fieldAgreementMap = new ArrayListValuedHashMap<>();
		for (Platformed<ClassNode> input : inputs) {
			for (Platformed<FieldNode> platformed : input.split(c -> c.fields, (c, f) -> f.invisibleAnnotations)) {
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
					clone.invisibleAnnotations.add(classInfo.createAnnotation());
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
		AnnotationNode node = new AnnotationNode(Classes.DISPLACE_DESC);
		node.visit("value", name);
		node.visitEnd();
		return node;
	}
}
