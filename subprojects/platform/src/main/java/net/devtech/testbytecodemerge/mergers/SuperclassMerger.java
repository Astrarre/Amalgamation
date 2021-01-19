package net.devtech.testbytecodemerge.mergers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.devtech.testbytecodemerge.ClassInfo;
import io.github.f2bb.amalgamation.Parent;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class SuperclassMerger implements Merger {
	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		Map<String, List<ClassInfo>> supers = new HashMap<>();
		for (ClassInfo info : infos) {
			supers.computeIfAbsent(info.node.superName, s -> new ArrayList<>()).add(info);
		}

		// most common super class, this gets priority and is what is shown in the source
		String mostCommon = null;
		int count = 0;
		for (String s : supers.keySet()) {
			int size = supers.get(s).size();
			if (size > count) {
				mostCommon = s;
				count = size;
			}
		}

		if (mostCommon == null) {
			throw new IllegalStateException("no classes!");
		}

		node.superName = mostCommon;

		supers.remove(mostCommon);
		if (!supers.isEmpty()) {
			supers.forEach((s, i) -> {
				AnnotationVisitor n = node.visitAnnotation(Type.getDescriptor(Parent.class), true);
				AnnotationVisitor visitor = n.visitArray("platform");
				for (ClassInfo info : i) {
					visitor.visit("platform", info.createPlatformAnnotation());
				}

				n.visit("parent", Type.getObjectType(s));
			});
		}
	}
}
