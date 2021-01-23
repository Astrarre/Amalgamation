package io.github.f2bb.amalgamation.platform.util;

import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Set;

public class SplitterUtil {
	public static boolean matches(AnnotationNode node, Set<String> platforms) {
		if (ClassInfo.PLATFORM_DESC.equals(node.desc)) {
			return platforms.containsAll((List<String>) node.values.get(1));
		} else {
			return false;
		}
	}

	public static boolean matches(List<AnnotationNode> nodes, Set<String> platforms) {
		for (AnnotationNode node : nodes) {
			if (matches(node, platforms)) {
				return true;
			}
		}
		return false;
	}
}
