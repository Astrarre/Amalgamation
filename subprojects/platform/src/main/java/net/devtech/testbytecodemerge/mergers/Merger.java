package net.devtech.testbytecodemerge.mergers;

import java.util.List;

import net.devtech.testbytecodemerge.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public interface Merger extends Opcodes {
	void merge(ClassNode node, List<ClassInfo> infos);

	default Merger andThen(Merger merger) {
		return (node, infos) -> {
			this.merge(node, infos);
			merger.merge(node, infos);
		};
	}
}
