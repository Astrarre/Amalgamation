package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class HeaderMerger implements Merger {
	@Override
	public void merge(MergerContext mergerContext) {
		ClassNode root = mergerContext.getInfos().get(0).node;
		mergerContext.getNode().name = root.name;
		mergerContext.getNode().version = root.version;
		if (mergerContext.getInfos().size() != mergerContext.getAvailable().size()) {
			for (ClassInfo info : mergerContext.getInfos()) {
				if (mergerContext.getNode().visibleAnnotations == null) {
					mergerContext.getNode().visibleAnnotations = new ArrayList<>();
				}
				mergerContext.getNode().visibleAnnotations.add(info.createPlatformAnnotation());
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
