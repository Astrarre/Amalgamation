package io.github.astrarre.merger.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.merger.Classes;
import org.objectweb.asm.tree.AnnotationNode;

public class Platformed<T> implements Identified {
	public final PlatformId id;
	public final T val;

	public Platformed(PlatformId platforms, T val) {
		this.id = platforms;
		this.val = val;
	}

	public List<Platformed<T>> split(Function<T, List<AnnotationNode>> node) {
		List<Platformed<T>> instances = new ArrayList<>();
		for (PlatformId platform : getPlatforms(node.apply(this.val), this.id)) {
			instances.add(new Platformed<>(platform, this.val));
		}
		return instances;
	}

	public <D> List<Platformed<D>> split(Function<T, Iterable<D>> function, BiFunction<T, D, List<AnnotationNode>> nodeFunc) {
		List<Platformed<D>> instances = new ArrayList<>();
		for (D t : function.apply(this.val)) {
			List<AnnotationNode> nodes = nodeFunc.apply(this.val, t);
			for (PlatformId platform : getPlatforms(nodes, this.id)) {
				instances.add(new Platformed<>(platform, t));
			}
		}

		return instances;
	}

	public List<Platformed<T>> split(List<AnnotationNode> node) {
		List<Platformed<T>> instances = new ArrayList<>();
		for (PlatformId platform : getPlatforms(node, this.id)) {
			instances.add(new Platformed<>(platform, this.val));
		}
		return instances;
	}

	public static Iterable<PlatformId> getPlatforms(List<AnnotationNode> annotationNodes, PlatformId activePlatform) {
		if(annotationNodes == null) return Collections.singletonList(activePlatform);
		List<PlatformId> platforms = null;
		for (AnnotationNode node : annotationNodes) {
			if(Classes.PLATFORM_DESC.equals(node.desc)) {
				List<String> names = new ArrayList<>(AsmUtil.get(node, "value", Collections.emptyList()));
				names.addAll(activePlatform.names);
				PlatformId platform = new PlatformId(names);
				if(platforms == null) platforms = new ArrayList<>();
				platforms.add(platform);
			}
		}

		if(platforms == null) {
			return Collections.singletonList(activePlatform);
		} else {
			return platforms;
		}
	}

	@Override
	public PlatformId get() {
		return this.id;
	}

	public T getVal() {
		return this.val;
	}
}
