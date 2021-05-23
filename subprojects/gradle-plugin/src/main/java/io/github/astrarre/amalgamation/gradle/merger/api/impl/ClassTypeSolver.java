package io.github.astrarre.amalgamation.gradle.merger.api.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;

public class ClassTypeSolver implements TypeSolver {
	private static final String CLASS_EXTENSION = ".class";
	private final ClassPool classPool = new ClassPool();
	private final Map<String, String> knownClasses = new HashMap<>();
	private TypeSolver parent;

	public ClassTypeSolver(Collection<Path> paths, Set<String> allPaths) throws IOException {
		for (Path path : paths) {
			Files.walk(path)
			     .filter(Files::isRegularFile)
			     .map(Path::toString)
			     .map(p -> p.substring(1))
			     .peek(allPaths::add)
			     .filter(p -> p.endsWith(CLASS_EXTENSION))
			     .forEach(path1 -> this.knownClasses.put(convertEntryPathToClassName(path1), convertEntryPathToClassPoolName(path1)));
		}
		this.classPool.appendClassPath(new ClassPath() {
			@Override
			public InputStream openClassfile(String s) throws NotFoundException {
				for (Path path : paths) {
					Path file = path.resolve(s);
					if (Files.exists(file)) {
						try {
							return new BufferedInputStream(Files.newInputStream(file));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
				throw new NotFoundException(s);
			}

			@Override
			public URL find(String s) {
				for (Path path : paths) {
					Path file = path.resolve(s);
					if (Files.exists(file)) {
						try {
							return file.toUri().toURL();
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				}
				return null;
			}
		});
	}

	private static String convertEntryPathToClassName(String entryPath) {
		if (!entryPath.endsWith(CLASS_EXTENSION)) {
			throw new IllegalArgumentException(String.format("The entry path should end with %s", CLASS_EXTENSION));
		}
		String className = entryPath.substring(0, entryPath.length() - CLASS_EXTENSION.length());
		className = className.replace('/', '.');
		className = className.replace('$', '.');
		return className;
	}

	private static String convertEntryPathToClassPoolName(String entryPath) {
		if (!entryPath.endsWith(CLASS_EXTENSION)) {
			throw new IllegalArgumentException(String.format("The entry path should end with %s", CLASS_EXTENSION));
		}
		String className = entryPath.substring(0, entryPath.length() - CLASS_EXTENSION.length());
		return className.replace('/', '.');
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
		String storedKey = this.knownClasses.get(name);
		// If the name is not registered in the list we can safely say is not solvable here
		if (storedKey == null) {
			return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
		}

		try {
			return SymbolReference.solved(JavassistFactory.toTypeDeclaration(
					this.classPool.get(storedKey),
					this.getRoot()));
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
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
