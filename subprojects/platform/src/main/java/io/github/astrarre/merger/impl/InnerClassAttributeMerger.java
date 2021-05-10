package io.github.astrarre.merger.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.merger.Merger;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

public class InnerClassAttributeMerger extends Merger {
	public InnerClassAttributeMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations) {
		inputs.stream()
		      .map(Platformed::getVal)
		      .map(node -> node.innerClasses)
		      .flatMap(List::stream)
		      .map(InnerClassNodeWrapper::new)
		      .distinct()
		      .map(InnerClassNodeWrapper::getNode)
		      .forEach(node -> target.innerClasses.add(node));
	}

	public static class InnerClassNodeWrapper {
		private final InnerClassNode node;

		public InnerClassNodeWrapper(InnerClassNode node) {
			this.node = node;
		}

		@Override
		public int hashCode() {
			return this.node != null ? this.node.hashCode() : 0;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof InnerClassNodeWrapper)) {
				return false;
			}

			InnerClassNodeWrapper wrapper = (InnerClassNodeWrapper) object;
			return equals(this.node, wrapper.node);
		}

		private static boolean equals(InnerClassNode a, InnerClassNode b) {
			boolean result = a.name.equals(b.name);

			if (a.access != b.access) {
				// TODO: warning(a.name + " incompatible change: inner class access " + a.access + " =/= " + b.access);
			}

			return result;
		}

		public InnerClassNode getNode() {
			return this.node;
		}
	}
}
