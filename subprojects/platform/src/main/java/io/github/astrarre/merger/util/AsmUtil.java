package io.github.astrarre.merger.util;

import java.util.List;
import java.util.function.Predicate;

import org.objectweb.asm.tree.AnnotationNode;

public class AsmUtil {
	public static AnnotationNode withDesc(List<AnnotationNode> nodes, String desc, Predicate<AnnotationNode> predicate) {
		if(nodes == null) return null;
		for (AnnotationNode node : nodes) {
			if(desc.equals(node.desc) && predicate.test(node)) {
				return node;
			}
		}
		return null;
	}

	public static <T> T get(AnnotationNode node, String id, T def) {
		if(node == null) return def;
		int index = node.values.indexOf(id);
		if(index == -1) return def;
		return (T) node.values.get(index + 1);
	}

	public static boolean is(AnnotationNode node, String id, Object val) {
		if(node == null) return false;
		int index = node.values.indexOf(id);
		if(index == -1) return false;
		return val.equals(node.values.get(index + 1));
	}
}
