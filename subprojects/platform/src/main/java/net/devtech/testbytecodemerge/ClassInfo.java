package net.devtech.testbytecodemerge;

import net.devtech.testbytecodemerge.annotation.Platform;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class ClassInfo {
	public final ClassNode node;
	public final String[] names;

	public ClassInfo(ClassNode node, String[] names) {
		this.node = node;
		this.names = names;
	}

	public AnnotationNode createPlatformAnnotation() {
		AnnotationNode node = new AnnotationNode(Type.getDescriptor(Platform.class));
		AnnotationVisitor array = node.visitArray("value");
		for (String s : this.names) {
			array.visit("value", s);
		}
		return node;
	}
}
