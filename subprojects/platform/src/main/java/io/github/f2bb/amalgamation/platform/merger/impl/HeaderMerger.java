package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.tree.ClassNode;

public class HeaderMerger implements Merger {
	@Override
	public void merge(MergerConfig mergerConfig, ClassNode merged, List<ClassInfo> components) {
		ClassNode root = components.get(0).node;
		merged.name = root.name;
		merged.version = root.version;
		if (components.size() != mergerConfig.getAvailable().size()) {
			for (ClassInfo info : components) {
				if (merged.visibleAnnotations == null) {
					merged.visibleAnnotations = new ArrayList<>();
				}
				merged.visibleAnnotations.add(info.createPlatformAnnotation());
			}
		}
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		if (in.visibleAnnotations == null) {
			in.visibleAnnotations = new ArrayList<>();
		}

		boolean notStripped = SplitterUtil.matches(in.visibleAnnotations, available);
		if(notStripped) {
			in.visibleAnnotations.removeIf(annotation -> ClassInfo.PLATFORM.equals(annotation.desc));
		}
		return !notStripped;
	}
}
