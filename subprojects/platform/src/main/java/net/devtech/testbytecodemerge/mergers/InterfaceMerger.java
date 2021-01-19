package net.devtech.testbytecodemerge.mergers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.devtech.testbytecodemerge.ClassInfo;
import io.github.f2bb.amalgamation.Interface;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceMerger implements Merger {
	public static final String INTERFACE = Type.getDescriptor(Interface.class);

	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		Map<String, List<ClassInfo>> interfaces = new HashMap<>();
		for (ClassInfo info : infos) {
			for (String anInterface : info.node.interfaces) {
				interfaces.computeIfAbsent(anInterface, s -> new ArrayList<>()).add(info);
			}
		}

		interfaces.forEach((s, i) -> {
			node.interfaces.add(s);
			if(i.size() == infos.size()) return;

			AnnotationVisitor n = node.visitAnnotation(INTERFACE, true);
			AnnotationVisitor visitor = n.visitArray("platform");
			for (ClassInfo info : i) {
				visitor.visit("platform", info.createPlatformAnnotation());
			}

			n.visit("parent", Type.getObjectType(s));
		});
	}
}
