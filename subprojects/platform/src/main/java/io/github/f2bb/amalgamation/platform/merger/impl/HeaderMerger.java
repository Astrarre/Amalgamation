package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class HeaderMerger implements Merger {
	@Override
	public void merge(ClassNode node, List<ClassInfo> infos, Set<PlatformData> available) {
		ClassNode root = infos.get(0).node;
		node.name = root.name;
		node.version = root.version;
		if (infos.size() != available.size()) {
			for (ClassInfo info : infos) {
				if (node.visibleAnnotations == null) {
					node.visibleAnnotations = new ArrayList<>();
				}
				node.visibleAnnotations.add(info.createPlatformAnnotation());
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
			Iterator<AnnotationNode> iterator = in.visibleAnnotations.iterator();
			while (iterator.hasNext()) {
				AnnotationNode annotation = iterator.next();
				if(ClassInfo.PLATFORM.equals(annotation.desc)) {
					iterator.remove();
				}
			}
		}
		return !notStripped;
	}
}
