package io.github.astrarre.merger.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.merger.Merger;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
import org.objectweb.asm.tree.ClassNode;

public class HeaderMerger extends Merger {
	public HeaderMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations) {
		ClassNode copyTo = inputs.get(0).val;
		target.name = copyTo.name;
		int maxVersion = 0;
		for (Platformed<ClassNode> input : inputs) {
			if(input.val.version > maxVersion) {
				maxVersion = input.val.version;
			}
		}
		target.version = maxVersion;
	}
}