package io.github.astrarre.amalgamation.gradle.platform.merger.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.InterfaceAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceMerger extends Merger {
	public InterfaceMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(List<RawPlatformClass> inputs,
			ClassNode target,
			Map<String, List<String>> platformCombinations,
			List<AnnotationHandler> annotationHandlers) {
		Set<PlatformId> allPlatforms = new HashSet<>();
		MultiValuedMap<String, PlatformId> interfacesByPlatform = new ArrayListValuedHashMap<>();
		for (RawPlatformClass input : inputs) {
			for (Platformed<String> platformed : input.split(annotationHandlers, c -> c.interfaces, (c, s) -> MergeUtil.get(
					MergeUtil.withDesc(c.invisibleAnnotations,
							Constants.INTERFACE_DESC,
							node -> MergeUtil.is(node, "parent", Type.getObjectType(s))),
					"platforms",
					Collections.emptyList()))) {
				allPlatforms.add(platformed.id);
				interfacesByPlatform.put(platformed.val, platformed.id);
			}
		}
		interfacesByPlatform.asMap().forEach((s, names) -> {
			if (names.size() != allPlatforms.size()) { // not fully common interface
				for (AnnotationHandler handler : annotationHandlers) {
					if(handler instanceof InterfaceAnnotationHandler) {
						AnnotationNode node = ((InterfaceAnnotationHandler) handler).create(names, s);
						if(node != null) {
							if (target.invisibleAnnotations == null)
								target.invisibleAnnotations = new ArrayList<>();
							target.invisibleAnnotations.add(node);
							break;
						}
					}
				}
			}

			target.interfaces.add(s);
		});
	}
}