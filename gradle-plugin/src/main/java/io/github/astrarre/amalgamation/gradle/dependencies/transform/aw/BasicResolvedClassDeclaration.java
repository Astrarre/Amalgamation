package io.github.astrarre.amalgamation.gradle.dependencies.transform.aw;

import java.util.HashSet;
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

	static class Solver implements TypeSolver {
		Set<String> types = new HashSet<>();

		TypeSolver parent;

		@Override
		public TypeSolver getParent() {
			return this.parent;
		}

		@Override
		public void setParent(TypeSolver parent) {
			this.parent = parent;
		}

		@Override
		public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
			boolean found = false;
			if(this.types.contains(name)) {
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

		public void add(String type) {
			this.types.add(type.replace('/', '.').replace('$', '.'));
		}
	}

	public static void main(String[] args) {
		JavaParser parser = new JavaParser();
		CombinedTypeSolver solver = new CombinedTypeSolver();
		BasicResolvedClassDeclaration.Solver basic = new BasicResolvedClassDeclaration.Solver();
		basic.add("test/hello/Hello");
		solver.add(new ClassLoaderTypeSolver(AccessWidenerTransform.class.getClassLoader()));
		solver.add(basic);
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(solver);
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);
		ParseResult<CompilationUnit> parse = parser.parse("package e; import test.bruh.*; import test.hello.*; public class Test {Hello hello;}");
		System.out.println(parse.getProblems());
		parse.getResult().ifPresent(c -> {
			System.out.println("aaa " + c.getPrimaryType());
			for(TypeDeclaration<?> type : c.getTypes()) {
				System.out.println("bbb");
				for(FieldDeclaration field : type.getFields()) {
					System.out.println("ccc");
					System.out.println(field.getCommonType().asClassOrInterfaceType().resolve().getQualifiedName());
				}
			}
		});
	}
}
