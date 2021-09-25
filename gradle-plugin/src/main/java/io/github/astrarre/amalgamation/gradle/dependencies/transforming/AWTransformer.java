package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;

public class AWTransformer implements Transformer {
	protected final File aw;
	protected final AccessWidener widener;

	public AWTransformer(File aw) throws IOException {
		this.aw = aw;
		AccessWidener widener = new AccessWidener();
		AccessWidenerReader reader = new AccessWidenerReader(widener);
		try(BufferedReader input = Files.newBufferedReader(aw.toPath())) {
			reader.read(input);
		}
		this.widener = widener;
	}

	@Override
	public void apply(ClassNode node) {
	}

	@Override
	public void hash(Hasher hasher) {
		AmalgIO.hash(hasher, this.aw);
	}

	@Override
	public byte[] processResource(String path, byte[] input) {
		ClassWriter writer = new ClassWriter(0);
		ClassVisitor visitor = AccessWidenerVisitor.createClassVisitor(Opcodes.ASM9, writer, this.widener);
		ClassReader reader = new ClassReader(input);
		reader.accept(visitor, 0);
		return writer.toByteArray();
	}
}
