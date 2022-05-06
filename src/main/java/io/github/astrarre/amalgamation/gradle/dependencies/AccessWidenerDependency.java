package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
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
	protected VirtualPath evaluatePath(byte[] hash) throws IOException {
		return AmalgIO.DISK_OUT.outputDir(this.dirs.aws(this.project), AmalgIO.b64(hash));
	}

	public record FsPair(VirtualSource input, VirtualSink output) {}

	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws Exception {
		Set<Artifact> artifacts = new HashSet<>();
		List<FsPair> systems = new ArrayList<>();
		for(Artifact artifact : this.artifacts(this.widen, true)) {
			Artifact out = artifact.deriveMaven(this.dirs.aws(this.project), this.getCurrentHash());
			artifacts.add(out);
			if(out.equals(artifact)) {
				continue;
			}
			if(isOutdated) { // todo sources not appearing?
				AmalgIO.DISK_OUT.delete(out.file);
				AmalgIO.DISK_OUT.createIfAbsent(out.file.getParent());
				AmalgIO.DISK_OUT.copy(artifact.file, out.file);
				systems.add(new FsPair(artifact.file.openOrThrow(), AmalgIO.DISK_OUT.subsink(out.file)));
			}
		}

		if(isOutdated) {
			AccessWidener widener = new AccessWidener(true);
			AccessWidenerReader aw = new AccessWidenerReader(widener);
			for(Object accessWidener : this.accessWideners) {
				for(Artifact artifact : this.artifacts(accessWidener, true)) {
					try(var reader = artifact.file.asFile().newReader(StandardCharsets.UTF_8)) {
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
					VirtualPath cls = pair.input.find(path + ".class");
					VirtualPath java = pair.input.find(path + ".java");
					if(cls != null) {
						byte[] buf = cls.asFile().allBytes();
						ClassReader reader = new ClassReader(buf);
						ClassWriter writer = new ClassWriter(0);
						ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, widener);
						reader.accept(visitor, 0);
						VirtualFile outCls = pair.output.outputFile(path + ".class");
						AmalgIO.DISK_OUT.write(outCls, ByteBuffer.wrap(writer.toByteArray()));
					}
					if(java != null) {
						ParseResult<CompilationUnit> parse = parser.parse(java.asFile().asString(StandardCharsets.UTF_8));
						if(!parse.isSuccessful()) {
							for(Problem problem : parse.getProblems()) {
								System.err.println(problem.getVerboseMessage());
							}
						} else {
							CompilationUnit unit = parse.getResult().orElseThrow();
							CompilationUnit lpp = LexicalPreservingPrinter.setup(unit);
							transformer.transform(lpp);
							VirtualFile outJav = pair.output.outputFile(path + ".java");
							AmalgIO.DISK_OUT.writeString(outJav, LexicalPreservingPrinter.print(lpp), StandardCharsets.UTF_8);
						}
					}
				}
			}
		}

		for(FsPair system : systems) {
			system.input.close();
			system.output.close();
		}
		return artifacts;
	}
}
