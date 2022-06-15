package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import io.github.astrarre.amalgamation.gradle.utils.zip.ZipIO;
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
	public AmalgDirs dirs = AmalgDirs.ROOT_PROJECT;

	public AccessWidenerDependency(Project project, Object widen) {
		super(project);
		this.widen = widen;
	}

	public void accessWidener(Object object) {
		this.accessWideners.add(this.of(object));
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		// seperate aw hash is in order i rekon
		for(Object widener : this.accessWideners) {
			this.hashDep(hasher, widener);
		}

		this.hashDep(hasher, this.widen);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.dirs.aws(this.project).resolve(AmalgIO.b64(hash));
	}

	public record FsPair(FSRef input, FSRef output) {}

	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws Exception {
		Set<Artifact> artifacts = new HashSet<>();
		List<FsPair> systems = new ArrayList<>();
		for(Artifact in : this.artifacts(this.widen)) {
			Artifact out = in.deriveMaven(this.dirs.aws(this.project), this.getCurrentHash());
			artifacts.add(out);
			if(out.equals(in)) {
				continue;
			}
			if(isOutdated) {
				Files.deleteIfExists(out.file);
				AmalgIO.createParent(out.file);
				Files.copy(in.file, out.file);
				systems.add(new FsPair(ZipIO.readZip(in.file), ZipIO.createZip(out.file)));
			}
		}

		if(isOutdated) {
			AccessWidener widener = new AccessWidener(true);
			AccessWidenerReader aw = new AccessWidenerReader(widener);
			for(Object accessWidener : this.accessWideners) {
				for(Artifact artifact : this.artifacts(accessWidener)) {
					try(var reader = Files.newBufferedReader(artifact.file)) {
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
						ByteBuffer buf = AmalgIO.readAll(cls);
						ClassReader reader = new ClassReader(buf.array(), buf.arrayOffset(), buf.limit());
						ClassWriter writer = new ClassWriter(0);
						ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, widener);
						reader.accept(visitor, 0);
						Path outCls = pair.output.getPath(path + ".class");
						AmalgIO.write(outCls, ByteBuffer.wrap(writer.toByteArray()));
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
							Path outJava = pair.output.getPath(path + ".java");
							Files.writeString(outJava, LexicalPreservingPrinter.print(lpp), StandardCharsets.UTF_8);
						}
					}
				}
			}
		}

		for(FsPair system : systems) {
			system.output.flush();
		}
		return artifacts;
	}
}
