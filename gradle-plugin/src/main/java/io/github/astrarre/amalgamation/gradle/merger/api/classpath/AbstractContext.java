package io.github.astrarre.amalgamation.gradle.merger.api.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public abstract class AbstractContext implements Context {
	public final Map<String, ClassNode> classCache;
	public final Map<String, CompilationUnit> compilationUnitCache;
	public final Map<String, TypeDeclaration<?>> javaCache;
	protected final JavaParser parser;

	public AbstractContext(JavaParser parser) {
		this(new HashMap<>(), new HashMap<>(), new HashMap<>(), parser);
	}

	public AbstractContext(Map<String, ClassNode> cache,
			Map<String, CompilationUnit> unit,
			Map<String, TypeDeclaration<?>> javaCache,
			JavaParser parser) {
		this.classCache = cache;
		this.compilationUnitCache = unit;
		this.javaCache = javaCache;
		this.parser = parser;
	}

	public static Optional<String> getInternalName(TypeDeclaration<?> declaration) {
		if (declaration.isTopLevelType()) {
			return declaration.findCompilationUnit()
			                  .map(cu -> cu.getPackageDeclaration()
			                               .map(NodeWithName::getNameAsString)
			                               .map(n -> n.replace('.', '/'))
			                               .map(pkg -> pkg + "/" + declaration.getNameAsString())
			                               .orElse(declaration.getNameAsString()));
		}
		return declaration.findAncestor(TypeDeclaration.class)
		                  .map(td -> (TypeDeclaration<?>) td)
		                  .flatMap(td -> td.getFullyQualifiedName().map(fqn -> fqn + "$" + declaration.getNameAsString()));
	}

	// todo Type#getDescriptor
	@Override
	public ClassNode getClass(String internalName) {
		return this.classCache.computeIfAbsent(internalName, s -> {
			try {
				InputStream stream = this.getResource(s + ".class");
				if(stream == null) return null;
				ClassReader reader = new ClassReader(stream);
				ClassNode node = new ClassNode();
				reader.accept(node, 0);
				return node;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public TypeDeclaration<?> getJava(ClassSourceInfo info) {
		return this.javaCache.computeIfAbsent(info.internalName, s -> {
			int last = Math.max(s.lastIndexOf('/'), s.lastIndexOf('$'));
			if (last == -1) {
				last = 0;
			}

			CompilationUnit unit = this.get(info);
			for (TypeDeclaration<?> type : unit.getTypes()) {
				String internalName = getInternalName(type).orElse(null);
				if(s.equals(internalName)) {
					return type;
				}
			}
			return null;
		});
	}


	public CompilationUnit get(ClassSourceInfo info) {
		String internalName = info.internalName;
		CompilationUnit unit = this.getNormal(internalName);
		if (unit == null) {
			/*int pkgIndex = internalName.lastIndexOf('/');
			String pkg;
			if(pkgIndex == -1) {
				pkg = "";
			} else {
				pkg = internalName.substring(0, pkgIndex);
			}

			for (String fileName : this.fileNames(internalName)) {
				CompilationUnit newUnit = this.parse(fileName);
				for (TypeDeclaration<?> type : newUnit.getTypes()) {
				}
			}*/
		}
		return unit;
	}

	/**
	 * doesn't handle multiple class files defined in the same java file (does handle inner classes)
	 *
	 * @see Document yep that's valid java
	 */
	protected CompilationUnit getNormal(String internalName) {
		int index = internalName.indexOf('$', internalName.lastIndexOf('/') + 1);
		String root;
		if (index == -1) {
			root = internalName;
		} else {
			root = internalName.substring(0, index);
		}

		return this.compilationUnitCache.computeIfAbsent(root, name -> this.parse(name + ".java"));
	}

	public CompilationUnit parse(String fileName) {
		InputStream input = this.getResource(fileName);
		if (input != null) {
			return handle(this.parser.parse(input));
		}
		return null;
	}

	public static CompilationUnit handle(ParseResult<CompilationUnit> result) {
		for (Problem problem : result.getProblems()) {
			problem.getLocation().ifPresent(System.err::println);
			problem.getCause().ifPresent(Throwable::printStackTrace);
		}
		if (!result.getProblems().isEmpty()) {
			throw new IllegalStateException("Unable to parse!");
		}

		return result.getResult().orElseThrow(IllegalStateException::new);
	}

}
