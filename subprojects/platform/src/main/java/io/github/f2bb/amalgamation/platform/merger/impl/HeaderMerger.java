package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.List;
import java.util.Set;

import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.tree.ClassNode;

public class HeaderMerger implements Merger {
	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		ClassNode root = infos.get(0).node;
		node.name = root.name;
		node.version = root.version;
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		return SplitterUtil.matches(in.visibleAnnotations, available);
	}
}
