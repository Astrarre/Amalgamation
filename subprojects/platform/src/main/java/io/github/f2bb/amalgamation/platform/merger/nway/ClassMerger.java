package io.github.f2bb.amalgamation.platform.merger.nway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.util.asm.InsnUtil;
import io.github.f2bb.amalgamation.platform.util.asm.desc.Desc;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class ClassMerger {
	private final Map<PlatformData, ClassNode> nodes;

	public ClassMerger(Map<PlatformData, ClassNode> nodes) {this.nodes = nodes;}

	public Map<Desc, List<PlatformData>> findMethods() {
		Map<Desc, List<PlatformData>> map = new HashMap<>();
		this.nodes.forEach((p, m) -> m.methods.stream()
		                                      .map(method -> new Desc(method.name, method.desc, Desc.METHOD))
		                                      .forEach(d -> map.computeIfAbsent(d, e -> new ArrayList<>()).add(p)));
		return map;
	}


	/**
	 * @param node the merging classnode
	 * @param data the platforms for which a method with this exact descriptor exists
	 * @param desc the descriptor itself
	 */
	public void mergeMethods(ClassNode node, List<PlatformData> data, Desc desc) {
		Map<MethodNode, List<PlatformData>> agreed = this.computeAgreement(data, desc);

		// find the most agreed opon bytecode for the method
		MethodNode mostAgreed = null;
		int quantityAgreed = 0;
		for (Map.Entry<MethodNode, List<PlatformData>> entry : agreed.entrySet()) {
			MethodNode m = entry.getKey();
			int q = entry.getValue().size();
			if (q > quantityAgreed) {
				mostAgreed = m;
				quantityAgreed = q;
			}
		}


		MethodNode added = (MethodNode) node.visitMethod(mostAgreed.access,
				mostAgreed.name,
				mostAgreed.desc,
				mostAgreed.signature,
				mostAgreed.exceptions.toArray(new String[0]));
		mostAgreed.accept(added);
		agreed.remove(mostAgreed).stream().map(PlatformData::createNode).forEach(added.visibleAnnotations::add);

		AtomicInteger counter = new AtomicInteger();
		agreed.forEach((m, p) -> {
			MethodNode extra = (MethodNode) node.visitMethod(m.access,
					String.format("%s-$%d$", m.name, counter.incrementAndGet()),
					m.desc,
					m.signature,
					m.exceptions.toArray(new String[0]));
			p.stream().map(PlatformData::createNode).forEach(extra.visibleAnnotations::add);
		});
	}

	/**
	 * finds all of the platforms that agree on the same bytecode
	 */
	private Map<MethodNode, List<PlatformData>> computeAgreement(List<PlatformData> data, Desc desc) {
		Map<MethodNode, List<PlatformData>> agreed = new HashMap<>();
		for (PlatformData platform : data) {
			MethodNode node = this.getMethod(platform, desc);
			boolean new_ = true;
			for (MethodNode comp : agreed.keySet()) {
				if (InsnUtil.areInstructionsEqual(comp, node)) {
					new_ = false;
					agreed.computeIfAbsent(comp, k -> new ArrayList<>()).add(platform);
					break;
				}
			}

			if(new_) agreed.computeIfAbsent(node, e -> new ArrayList<>()).add(platform);
		}
		return agreed;
	}

	public MethodNode getMethod(PlatformData platform, Desc d) {
		for (MethodNode method : this.nodes.get(platform).methods) {
			if(method.desc.equals(d.desc) && method.name.equals(d.name)) {
				return method;
			}
		}

		throw new IllegalArgumentException(d + " not found in " + this.nodes.get(platform).name);
	}
}
