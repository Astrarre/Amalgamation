package io.github.astrarre.amalgamation.gradle.merger.api.classpath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.objectweb.asm.tree.ClassNode;

public class PathsContext extends AbstractContext {
	public final Collection<Path> data;

	public PathsContext(JavaParser parser, Collection<Path> data) {
		super(parser);
		this.data = data;
	}

	public PathsContext(Map<String, ClassNode> cache,
			Map<String, CompilationUnit> unit,
			Map<String, TypeDeclaration<?>> javaCache,
			JavaParser parser, Collection<Path> data) {
		super(cache, unit, javaCache, parser);
		this.data = data;
	}

	@Override
	public InputStream getResource(String fileName) {
		for (Path path : this.data) {
			Path file = path.resolve(fileName);
			if (Files.exists(file)) {
				try {
					return new BufferedInputStream(Files.newInputStream(file));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return null;
	}
}
