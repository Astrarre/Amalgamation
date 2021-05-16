package io.github.astrarre.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.astrarre.Classes;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.merger.context.PlatformData;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class RawPlatformClass implements Identified {
	public final PlatformId id;
	public final ClassNode val;
	public final PlatformData data;

	public RawPlatformClass(PlatformId platforms, ClassNode val, PlatformData data) {
		this.id = platforms;
		this.val = val;
		this.data = data;
	}

	public List<Platformed<ClassNode>> split(Function<ClassNode, List<AnnotationNode>> node) {
		List<Platformed<ClassNode>> instances = new ArrayList<>();
		for (PlatformId platform : getPlatforms(node.apply(this.val), this.id)) {
			instances.add(new Platformed<>(platform, this.val));
		}
		return instances;
	}

	public <D> List<Platformed<D>> split(Function<ClassNode, Iterable<D>> function, BiFunction<ClassNode, D, List<AnnotationNode>> nodeFunc) {
		List<Platformed<D>> instances = new ArrayList<>();
		for (D t : function.apply(this.val)) {
			List<AnnotationNode> nodes = nodeFunc.apply(this.val, t);
			for (PlatformId platform : getPlatforms(nodes, this.id)) {
				instances.add(new Platformed<>(platform, t));
			}
		}

		return instances;
	}

	public List<Platformed<ClassNode>> split(List<AnnotationNode> node) {
		List<Platformed<ClassNode>> instances = new ArrayList<>();
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
				if(platforms == null) platforms = new ArrayList<>();
				platforms.add(getPlatform(node, activePlatform));
			}
		}

		if(platforms == null) {
			return Collections.singletonList(activePlatform);
		} else {
			return platforms;
		}
	}

	public static PlatformId getPlatform(AnnotationNode node, PlatformId active) {
		List<String> names = new ArrayList<>(AsmUtil.get(node, "value", Collections.emptyList()));
		names.addAll(active.names);
		return new PlatformId(names);
	}

	@Override
	public PlatformId get() {
		return this.id;
	}

	public ClassNode getVal() {
		return this.val;
	}
}
