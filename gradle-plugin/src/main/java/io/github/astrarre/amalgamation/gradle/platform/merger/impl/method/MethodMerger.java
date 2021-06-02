package io.github.astrarre.amalgamation.gradle.platform.merger.impl.method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.field.FieldKey;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.field.FieldMerger;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodMerger extends Merger {
	protected final boolean compareInstructions;

	public MethodMerger(Map<String, ?> properties) {
		super(properties);
		this.compareInstructions = (Boolean) properties.get("compareInstructions");
	}

	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			AnnotationHandler handler) {
		Set<PlatformId> all = new HashSet<>();
		MultiValuedMap<MethodKey, PlatformId> methodAgreementMap = new ArrayListValuedHashMap<>();
		for (RawPlatformClass input : inputs) {
			for (MethodNode method : input.val.methods) {
				boolean visited = false;
				if(method.invisibleAnnotations != null) {
					for (AnnotationNode annotation : method.invisibleAnnotations) {
						PlatformId id = handler.parseMethodPlatforms(annotation);
						if(id != null) {
							methodAgreementMap.put(new MethodKey(compareInstructions, method), id);
							all.add(id);
							visited = true;
						}
					}
				}

				if(!visited) {
					methodAgreementMap.put(new MethodKey(compareInstructions, method), input.id);
					all.add(input.id);
				}
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
				if(current.startsWith("<init>")) current = current.replace("<init>", "newInstance");
				else if(current.startsWith("<clinit>")) current = current.replace("<clinit>", "staticInit");

				if(clone.invisibleAnnotations == null) clone.invisibleAnnotations = new ArrayList<>();
				clone.invisibleAnnotations.add(FieldMerger.displace(key.node.name));
				clone.name = current;
			}

			if (ids.size() != all.size()) {
				if (clone.invisibleAnnotations == null) {
					clone.invisibleAnnotations = new ArrayList<>();
				}

				for (PlatformId classInfo : ids) {
					clone.invisibleAnnotations.add(handler.createMethodPlatformAnnotation(classInfo));
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
