package io.github.astrarre.amalgamation.gradle.splitter.util;

import java.util.ArrayList;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.merger.api.Platformed;
import io.github.astrarre.amalgamation.gradle.utils.Classes;
import org.objectweb.asm.tree.AnnotationNode;

public class SplitterUtil {
	public static boolean matches(List<AnnotationNode> nodes, PlatformId id) {
		boolean visited = false;
		for (PlatformId platform : Platformed.getPlatforms(nodes, PlatformId.EMPTY)) {
			if(platform == PlatformId.EMPTY) continue;
			visited = true;
			if(platform.names.containsAll(id.names)) {
				return true;
			}
		}
		return !visited;
	}

	public static List<AnnotationNode> stripAnnotations(List<AnnotationNode> strip, PlatformId id) {
		List<AnnotationNode> nodes = new ArrayList<>();
		for (AnnotationNode node : strip) {
			if(Classes.PLATFORM_DESC.equals(node.desc)) {
				PlatformId platform = Platformed.getPlatform(node, PlatformId.EMPTY);
				if(platform.names.containsAll(id.names)) {
					List<String> stripped = new ArrayList<>(platform.names);
					stripped.removeAll(id.names);
					if(!stripped.isEmpty()) {
						PlatformId created = new PlatformId(stripped);
						nodes.add(created.createAnnotation());
					}
				}
			} else {
				nodes.add(node);
			}
		}
		return nodes;
	}
}
