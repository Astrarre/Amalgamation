package io.github.astrarre.merger.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.api.RawPlatformClass;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.Classes;
import io.github.astrarre.merger.Merger;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
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
	public void merge(List<RawPlatformClass> inputs, ClassNode target, Map<String, List<String>> platformCombinations) {
		Set<PlatformId> allPlatforms = new HashSet<>();
		MultiValuedMap<String, PlatformId> interfacesByPlatform = new ArrayListValuedHashMap<>();
		for (RawPlatformClass input : inputs) {
			for (Platformed<String> platformed : input.split(c -> c.interfaces, (c, s) -> AsmUtil.get(
					AsmUtil.withDesc(c.invisibleAnnotations,
							Classes.INTERFACE_DESC,
							node -> AsmUtil.is(node, "parent", Type.getObjectType(s))),
					"platforms",
					Collections.emptyList()))) {
				allPlatforms.add(platformed.id);
				interfacesByPlatform.put(platformed.val, platformed.id);
			}
		}
		interfacesByPlatform.asMap().forEach((s, names) -> {
			if (names.size() != allPlatforms.size()) { // not fully common interface
				List<AnnotationNode> nodes = new ArrayList<>();
				names.forEach(id -> nodes.add(id.createAnnotation())); // todo reducing
				AnnotationNode interfaceAnnotation = new AnnotationNode(Classes.INTERFACE_DESC);
				interfaceAnnotation.visit("parent", Type.getObjectType(s));
				interfaceAnnotation.visit("platforms", nodes);
				if(target.invisibleAnnotations == null) target.invisibleAnnotations = new ArrayList<>();
				target.invisibleAnnotations.add(interfaceAnnotation);
			}

			target.interfaces.add(s);
		});
	}
}
