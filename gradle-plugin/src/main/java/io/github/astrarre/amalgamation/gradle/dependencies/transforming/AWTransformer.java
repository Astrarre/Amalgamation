package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;

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
	public ClassNode apply(ClassNode node) {
		ClassNode n = new ClassNode();
		ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, n, this.widener);
		node.accept(visitor);
		return n;
	}

	@Override
	public void applyJava(CompilationUnit unit) {
		for(TypeDeclaration<?> type : unit.getTypes()) {
			String qualified = type.getFullyQualifiedName().orElseThrow();
			if(this.widener.getTargets().contains(qualified.replace('.', '/'))) {

			}
		}
	}

	@Override
	public void hash(Hasher hasher) {
		AmalgIO.hash(hasher, this.aw);
	}
}
