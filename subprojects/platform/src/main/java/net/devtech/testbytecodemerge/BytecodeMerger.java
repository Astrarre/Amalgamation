package net.devtech.testbytecodemerge;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import net.devtech.testbytecodemerge.mergers.AccessMerger;
import net.devtech.testbytecodemerge.mergers.InterfaceMerger;
import net.devtech.testbytecodemerge.mergers.Merger;
import net.devtech.testbytecodemerge.mergers.SignatureMerger;
import net.devtech.testbytecodemerge.mergers.SuperclassMerger;
import net.devtech.testbytecodemerge.mergers.field.FieldMerger;
import net.devtech.testbytecodemerge.mergers.method.MethodMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class BytecodeMerger {
	public static final Merger MERGER;
	static {
		Merger merger = (node, infos) -> {
			ClassNode root = infos.get(0).node;
			node.name = root.name;
			node.version = root.version;
		};
		merger = merger.andThen(new SuperclassMerger());
		merger = merger.andThen(new InterfaceMerger());
		merger = merger.andThen(new AccessMerger());
		merger = merger.andThen(new SignatureMerger());
		merger = merger.andThen(new MethodMerger());
		merger = merger.andThen(new FieldMerger());
		MERGER = merger;
	}

	public static void main(String[] args) throws IOException {
		ClassInfo infoA = new ClassInfo(readClass("ClassA"), new String[] {"A"});
		ClassInfo infoB = new ClassInfo(readClass("ClassB"), new String[] {"B"});

		ClassNode merged = new ClassNode();
		MERGER.merge(merged, Arrays.asList(infoA, infoB));

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		merged.accept(writer);

		FileOutputStream output = new FileOutputStream("out.class");
		output.write(writer.toByteArray());
		output.close();
	}

	private static ClassNode readClass(String name) throws IOException {
		FileInputStream fis = new FileInputStream("main/" + name + ".class");
		ClassReader reader = new ClassReader(fis);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);
		fis.close();
		return node;
	}
}
