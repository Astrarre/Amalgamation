package io.github.f2bb.amalgamation.platform.merger.impl;

import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import java.util.List;
import java.util.Set;

public class InnerClassAttributeMerger implements Merger {

	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		infos.stream()
				.map(ClassInfo::getNode)
				.map(c -> c.innerClasses)
				.flatMap(List::stream)
				.map(InnerClassNodeWrapper::new)
				.distinct()
				.map(InnerClassNodeWrapper::getNode)
				.forEach(n -> node.innerClasses.add(n));
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		return false;
	}

	static class InnerClassNodeWrapper {
		private final InnerClassNode node;

		InnerClassNodeWrapper(InnerClassNode node) {
			this.node = node;
		}

		private static boolean equals(InnerClassNode a, InnerClassNode b) {
			boolean result = a.name.equals(b.name);

			if (a.access != b.access) {
				// TODO: warning(a.name + " incompatible change: inner class access " + a.access + " =/= " + b.access);
			}

			return result;
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

		public InnerClassNode getNode() {
			return this.node;
		}
	}
}
