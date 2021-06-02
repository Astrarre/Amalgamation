package io.github.astrarre.amalgamation.gradle.platform.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import org.objectweb.asm.tree.AnnotationNode;

public class Platformed<T> implements Identified {
	public final PlatformId id;
	public final T val;

	public Platformed(PlatformId platforms, T val) {
		this.id = platforms;
		this.val = val;
	}

	public static boolean matches(List<AnnotationNode> nodes, PlatformId id, AnnotationHandler handlers) {
		boolean visited = false;
		for (PlatformId platform : getPlatforms(handlers, nodes, PlatformId.EMPTY)) {
			if(platform == PlatformId.EMPTY) continue;
			visited = true;
			if(platform.names.containsAll(id.names)) {
				return true;
			}
		}
		return !visited;
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

	public static Iterable<PlatformId> getPlatforms(AnnotationHandler handler, List<AnnotationNode> annotationNodes, PlatformId activePlatform) {
		if (annotationNodes == null) {
			return Collections.singletonList(activePlatform);
		}
		List<PlatformId> platforms = null;
		for (AnnotationNode node : annotationNodes) {
			PlatformId names = handler.parseClassPlatforms(node);
			if (names != null) {
				names = new PlatformId(names, activePlatform.names);
				if (platforms == null) {
					platforms = new ArrayList<>();
				}
				platforms.add(names);
			}
		}

		if (platforms == null) {
			return Collections.singletonList(activePlatform);
		} else {
			return platforms;
		}
	}

	public <D> List<Platformed<D>> split(AnnotationHandler handlers,
			Function<T, Iterable<D>> function,
			BiFunction<T, D, List<AnnotationNode>> nodeFunc) {
		List<Platformed<D>> instances = new ArrayList<>();
		for (D t : function.apply(this.val)) {
			List<AnnotationNode> nodes = nodeFunc.apply(this.val, t);
			for (PlatformId platform : getPlatforms(handlers, nodes, this.id)) {
				instances.add(new Platformed<>(platform, t));
			}
		}

		return instances;
	}

	public List<Platformed<T>> split(AnnotationHandler handlers, List<AnnotationNode> node) {
		List<Platformed<T>> instances = new ArrayList<>();
		for (PlatformId platform : getPlatforms(handlers, node, this.id)) {
			instances.add(new Platformed<>(platform, this.val));
		}
		return instances;
	}

	@Override
	public PlatformId get() {
		return this.id;
	}

	public T getVal() {
		return this.val;
	}
}
