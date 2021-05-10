package io.github.astrarre.merger.impl.method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.merger.Merger;
import io.github.astrarre.merger.api.PlatformId;
import io.github.astrarre.merger.api.Platformed;
import io.github.astrarre.merger.impl.field.FieldMerger;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodMerger extends Merger {
	protected final boolean compareInstructions;

	public MethodMerger(Map<String, ?> properties) {
		super(properties);
		this.compareInstructions = (Boolean) properties.get("compareInstructions");
	}

	@Override
	public void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations) {
		Set<PlatformId> all = new HashSet<>();
		MultiValuedMap<MethodKey, PlatformId> methodAgreementMap = new ArrayListValuedHashMap<>();
		for (Platformed<ClassNode> input : inputs) {
			for (Platformed<MethodNode> platformed : input.split(c -> c.methods, (c, f) -> f.invisibleAnnotations)) {
				methodAgreementMap.put(new MethodKey(this.compareInstructions, platformed.val), platformed.id);
				all.add(platformed.id);
			}
		}

		methodAgreementMap.asMap().forEach((key, ids) -> {
			MethodNode clone = new MethodNode(key.node.access, key.node.name, key.node.desc, key.node.signature, null);
			key.node.accept(new ClassVisitor(ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					return clone;
				}
			});

			int counter = 0;
			String current = key.node.name;
			while (this.hasMethod(target, current, key.node.desc)) {
				current = key.node.name + counter;
				counter++;
			}

			if (counter != 0) {
				clone.invisibleAnnotations.add(FieldMerger.displace(key.node.name));
			}

			if (ids.size() != all.size()) {
				if (clone.visibleAnnotations == null) {
					clone.visibleAnnotations = new ArrayList<>();
				}

				for (PlatformId classInfo : ids) {
					clone.visibleAnnotations.add(classInfo.createAnnotation());
				}
			}

			target.methods.add(clone);
		});
	}

	private boolean hasMethod(ClassNode target, String current, String desc) {
		for (MethodNode method : target.methods) {
			if (current.equals(method.name) && desc.equals(method.desc)) {
				return true;
			}
		}

		return false;
	}
}
