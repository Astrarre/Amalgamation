package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceMerger extends Merger {
	public InterfaceMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs, ClassNode target, Map<String, List<String>> platformCombinations, AnnotationHandler handler) {
		Set<PlatformId> allPlatforms = new HashSet<>();
		MultiValuedMap<String, PlatformId> interfacesByPlatform = new ArrayListValuedHashMap<>();
		for (RawPlatformClass input : inputs) {
			ClassNode c = input.val;
			Set<String> unannotatedInterfaces = new HashSet<>(c.interfaces);
			if(c.invisibleAnnotations != null) {
				for (AnnotationNode annotation : c.invisibleAnnotations) {
					Platformed<String> type = handler.parseInterfacePlatforms(annotation);
					if(type != null) {
						unannotatedInterfaces.remove(type.val);
						interfacesByPlatform.put(type.val, type.id);
						allPlatforms.add(type.id);
					}
				}
			}

			allPlatforms.add(input.id);
			for (String iface : unannotatedInterfaces) {
				interfacesByPlatform.put(iface, input.id);
			}
		}

		interfacesByPlatform.asMap().forEach((s, names) -> {
			if (names.size() != allPlatforms.size()) { // not fully common interface
				for (PlatformId name : names) {
					AnnotationNode node = handler.createInterfacePlatformAnnotation(new Platformed<>(name, s));
					if (node != null) {
						if (target.invisibleAnnotations == null) {
							target.invisibleAnnotations = new ArrayList<>();
						}
						target.invisibleAnnotations.add(node);
						break;
					}
				}
			}

			target.interfaces.add(s);
		});
	}
}
