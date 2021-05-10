package io.github.astrarre.merger.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.merger.Merger;
import io.github.astrarre.merger.api.PlatformId;
import io.github.astrarre.merger.api.Platformed;
import org.objectweb.asm.tree.ClassNode;

public class ClassMerger extends Merger {

	public ClassMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations) {
		Set<PlatformId> computed = new HashSet<>();
		List<Platformed<ClassNode>> recomputedInputs = new ArrayList<>();
		for (Platformed<ClassNode> input : inputs) {
			for (Platformed<ClassNode> platformed : input.split(n -> n.invisibleAnnotations)) {
				computed.add(platformed.id);
				recomputedInputs.add(platformed);
			}
		}

		inputs.clear();
		inputs.addAll(recomputedInputs);

		if (!allActivePlatforms.equals(computed)) {
			for (PlatformId ids : computed) {
				if(target.invisibleAnnotations == null) target.invisibleAnnotations = new ArrayList<>();
				target.invisibleAnnotations.add(ids.createAnnotation()); // todo reduce
			}
		}
	}
}
