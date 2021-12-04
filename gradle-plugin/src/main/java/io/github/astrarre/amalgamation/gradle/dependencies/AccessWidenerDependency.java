package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import io.github.astrarre.amalgamation.gradle.utils.javaparser.BasicResolvedClassDeclaration;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.javaparser.SourceAccessWidenerTransformer;

public class AccessWidenerDependency extends CachedDependency {
	public final Object widen;
	public final List<Object> accessWideners = new ArrayList<>();
	public final AmalgDirs dirs;

	public AccessWidenerDependency(Project project, Object widen, AmalgDirs dirs) {
		super(project);
		this.widen = widen;
		this.dirs = dirs;
	}

	public void accessWidener(Object object) {
		this.accessWideners.add(object);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.accessWideners);
		this.hashDep(hasher, this.widen);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.dirs.aws(this.project).resolve(AmalgIO.b64(hash));
	}

	public record FsPair(FileSystem input, FileSystem output) {}

	@Override
	protected List<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		List<Artifact> artifacts = new ArrayList<>();
		List<FsPair> systems = new ArrayList<>();
		for(Artifact artifact : this.artifacts(this.widen, true)) {
			Artifact out = artifact.deriveMaven(artifact.path, this.getCurrentHash());
			artifacts.add(out);
			if(isOutdated) {
				Files.copy(artifact.path, out.path);
				systems.add(new FsPair(U.openZip(artifact.path), U.openZip(out.path)));
			}
		}

		if(isOutdated) {
			AccessWidener widener = new AccessWidener(true);
			AccessWidenerReader aw = new AccessWidenerReader(widener);
			for(Object accessWidener : this.accessWideners) {
				for(Artifact artifact : this.artifacts(accessWidener, true)) {
					try(var reader = Files.newBufferedReader(artifact.path)) {
						aw.read(reader);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}

			SourceAccessWidenerTransformer transformer = new SourceAccessWidenerTransformer(widener);

			JavaParser parser = new JavaParser();
			CombinedTypeSolver solver = new CombinedTypeSolver();
			BasicResolvedClassDeclaration.Solver basic = new BasicResolvedClassDeclaration.Solver(systems);
			solver.add(new ClassLoaderTypeSolver(this.getClass().getClassLoader()));
			solver.add(basic);

			JavaSymbolSolver symbolSolver = new JavaSymbolSolver(solver);
			parser.getParserConfiguration().setSymbolResolver(symbolSolver);

			for(String target : widener.getTargets()) {
				String path = target.replace('.', '/');
				for(FsPair pair : systems) {
					Path cls = pair.input.getPath(path + ".class");
					Path java = pair.input.getPath(path + ".java");
					if(Files.exists(cls)) {
						byte[] buf = Files.readAllBytes(cls);
						ClassReader reader = new ClassReader(buf);
						ClassWriter writer = new ClassWriter(0);
						ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, widener);
						reader.accept(visitor, 0);
						Path outCls = pair.output.getPath(path + ".class");
						Files.write(outCls, writer.toByteArray());
					}
					if(Files.exists(java)) {
						ParseResult<CompilationUnit> parse = parser.parse(Files.readString(java));
						if(!parse.isSuccessful()) {
							for(Problem problem : parse.getProblems()) {
								System.err.println(problem.getVerboseMessage());
							}
						} else {
							CompilationUnit unit = parse.getResult().orElseThrow();
							CompilationUnit lpp = LexicalPreservingPrinter.setup(unit);
							transformer.transform(lpp);
							Path outJav = pair.output.getPath(path + ".java");
							Files.writeString(outJav, LexicalPreservingPrinter.print(lpp));
						}
					}
				}
			}
		}
		return artifacts;
	}
}
