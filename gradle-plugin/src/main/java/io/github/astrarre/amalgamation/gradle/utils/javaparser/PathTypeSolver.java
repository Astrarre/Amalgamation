package io.github.astrarre.amalgamation.gradle.utils.javaparser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class PathTypeSolver implements TypeSolver {
	protected final ClassPool pool = new ClassPool();
	protected final Path path;
	protected final Map<String, String> specialCases = new HashMap<>();
	protected TypeSolver parent;

	public PathTypeSolver(Path path) {
		this.path = path;
		for (Path file : UnsafeIterable.walkFiles(path)) {
			if(file.toString().endsWith(".class")) {
				try(InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
					ClassReader reader = new ClassReader(input);
					String internalName = reader.getClassName();
					if(internalName.indexOf('$') != -1) {
						this.specialCases.put(internalName.replace('$', '.').replace('/', '.'), internalName);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		this.pool.appendClassPath(new ClassPath() {
			@Override
			public InputStream openClassfile(String classFile) throws NotFoundException {
				try {
					return new BufferedInputStream(Files.newInputStream(path.resolve(classFile.replace('.', '/') + ".class")));
				} catch (IOException e) {
					throw new NotFoundException(classFile, e);
				}
			}

			@Override
			public URL find(String className) {
				try {
					return path.resolve(className.replace('.', '/') + ".class").toUri().toURL();
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Override
	public TypeSolver getParent() {
		return this.parent;
	}

	@Override
	public void setParent(TypeSolver parent) {
		Objects.requireNonNull(parent);
		if (this.parent != null) {
			throw new IllegalStateException("This TypeSolver already has a parent.");
		}
		if (parent == this) {
			throw new IllegalStateException("The parent of this TypeSolver cannot be itself.");
		}
		this.parent = parent;
	}

	@Override
	public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
		String fileName = this.specialCases.getOrDefault(name, name.replace('.', '/'));
		try {
			return SymbolReference.solved(JavassistFactory.toTypeDeclaration(this.pool.get(fileName), this.getRoot()));
		} catch (NotFoundException e) {
			// The names in stored key should always be resolved.
			// But if for some reason this happen, the user is notified.
			throw new IllegalStateException(String.format(
					"Unable to get class with name %s from class pool." + "This was not suppose to happen, please report at https://github" +
					".com/Astrarre/Amalgamation/issues",
					fileName));
		}
	}

	@Override
	public ResolvedReferenceTypeDeclaration solveType(String name) throws UnsolvedSymbolException {
		SymbolReference<ResolvedReferenceTypeDeclaration> ref = this.tryToSolveType(name);
		if (ref.isSolved()) {
			return ref.getCorrespondingDeclaration();
		} else {
			throw new UnsolvedSymbolException(name);
		}
	}
}
