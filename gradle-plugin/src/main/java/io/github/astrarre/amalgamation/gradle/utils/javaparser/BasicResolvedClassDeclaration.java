package io.github.astrarre.amalgamation.gradle.utils.javaparser;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import io.github.astrarre.amalgamation.gradle.dependencies.AccessWidenerDependency;

public class BasicResolvedClassDeclaration implements ResolvedClassDeclaration {
	final String internalName, packageName, qualifiedName, normalName;

	public BasicResolvedClassDeclaration(String name) {
		this.internalName = name;
		String qname = this.qualifiedName = name.replace('/', '.');
		this.packageName = qname.substring(0, qname.lastIndexOf('.'));
		this.normalName = qname.replace('$', '.');
	}

	@Override
	public String getName() {
		return this.normalName;
	}

	@Override
	public Optional<ResolvedReferenceType> getSuperClass() {
		return Optional.empty();
	}

	@Override
	public List<ResolvedReferenceType> getInterfaces() {
		return List.of();
	}

	@Override
	public List<ResolvedReferenceType> getAllSuperClasses() {
		return List.of();
	}

	@Override
	public List<ResolvedReferenceType> getAllInterfaces() {
		return List.of();
	}

	@Override
	public List<ResolvedConstructorDeclaration> getConstructors() {
		return List.of();
	}

	@Override
	public List<ResolvedReferenceType> getAncestors(boolean acceptIncompleteList) {
		return List.of();
	}

	@Override
	public List<ResolvedFieldDeclaration> getAllFields() {
		return List.of();
	}

	@Override
	public Set<ResolvedMethodDeclaration> getDeclaredMethods() {
		return Set.of();
	}

	@Override
	public Set<MethodUsage> getAllMethods() {
		return Set.of();
	}

	@Override
	public boolean isAssignableBy(ResolvedType type) {
		return false;
	}

	@Override
	public boolean isAssignableBy(ResolvedReferenceTypeDeclaration other) {
		return false;
	}

	@Override
	public boolean hasDirectlyAnnotation(String qualifiedName) {
		return false;
	}

	@Override
	public boolean isFunctionalInterface() {
		return false;
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		return AccessSpecifier.PUBLIC;
	}

	@Override
	public Optional<ResolvedReferenceTypeDeclaration> containerType() {
		return Optional.empty();
	}

	@Override
	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String getClassName() {
		return this.internalName;
	}

	@Override
	public String getQualifiedName() {
		return this.qualifiedName;
	}

	@Override
	public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
		return List.of();
	}

	public static class Solver implements TypeSolver {
		final List<AccessWidenerDependency.FsPair> types;

		TypeSolver parent;

		public Solver(List<AccessWidenerDependency.FsPair> types) {
			this.types = types;
		}

		@Override
		public TypeSolver getParent() {
			return this.parent;
		}

		@Override
		public void setParent(TypeSolver parent) {
			this.parent = parent;
		}

		boolean contains(String name) {
			for(AccessWidenerDependency.FsPair type : this.types) {
				if(Files.exists(type.input().getPath(name + ".class")) || Files.exists(type.input().getPath(name + ".java"))) {
					return true;
				}
			}
			return false;
		}
		@Override
		public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
			boolean found = false;
			if(contains(name)) {
				found = true;
			} else {
				int index;
				String str = name;
				while(!found && (index = str.lastIndexOf('.')) != -1) {
					found = this.tryToSolveType(str = str.substring(0, index)).isSolved();
				}
			}

			if(found) {
				return SymbolReference.solved(new BasicResolvedClassDeclaration(name));
			} else {
				return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
			}
		}
	}
}
