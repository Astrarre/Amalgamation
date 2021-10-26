package io.github.astrarre.amalgamation.gradle.dependencies.transform.aw;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.TransformDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.javaparser.SourceAccessWidenerTransformer;

public class AccessWidenerTransform implements TransformDependency.Transformer<AccessWidenerHelper> {
	final AccessWidener widener = new AccessWidener(true);
	File resolved;

	@Override
	public Class<AccessWidenerHelper> configurationHelper() {
		return AccessWidenerHelper.class;
	}

	@Override
	public <V> V process(Project project, Dependency dependency, InputType<V> type) throws IllegalArgumentException {
		if(type == AccessWidenerHelper.FILE) {
			AccessWidenerReader aw = new AccessWidenerReader(this.widener);
			File resolve = AmalgIO.resolve(project, dependency);
			this.resolved = resolve;
			try(var reader = Files.newBufferedReader(resolve.toPath())) {
				aw.read(reader);
			} catch(IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;
	}

	@Override
	public void configure(List<TransformDependency.Input<?>> inputs, ZipProcessBuilder builder) throws IOException {
		Objects.requireNonNull(this.widener, "no access widener dependency set!");
		SourceAccessWidenerTransformer transformer = new SourceAccessWidenerTransformer(this.widener);

		JavaParser parser = new JavaParser();
		CombinedTypeSolver solver = new CombinedTypeSolver();
		BasicResolvedClassDeclaration.Solver basic = new BasicResolvedClassDeclaration.Solver();
		solver.add(new ClassLoaderTypeSolver(this.getClass().getClassLoader()));
		solver.add(basic);

		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(solver);
		parser.getParserConfiguration().setSymbolResolver(symbolSolver);

		builder.setEntryProcessor(buffer -> {
			String path = buffer.path();
			if(path.endsWith(".java")) {
				String className = path.substring(0, path.length() - 5);
				basic.add(className);
				if(transformer.mayTransform(path)) {
					return ProcessResult.POST;
				} else {
					buffer.copyToOutput();
				}
			} else if(path.endsWith(".class")) {
				String className = path.substring(0, path.length() - 6);
				basic.add(className);
				if(this.widener.getTargets().contains(className.replace('/', '.'))) { // todo this no work
					ByteBuffer buf = buffer.read();
					ClassReader reader = new ClassReader(buf.array(), buf.arrayOffset(), buf.limit());
					ClassWriter writer = new ClassWriter(0);
					ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, this.widener);
					reader.accept(visitor, 0);
					buffer.writeToOutput(ByteBuffer.wrap(writer.toByteArray()));
				} else {
					buffer.copyToOutput();
				}
			} else {
				buffer.copyToOutput();
			}

			return ProcessResult.HANDLED;
		});
		builder.defaults().setPreEntryProcessor(o -> buffer -> {
			ParseResult<CompilationUnit> parse = parser.parse(StandardCharsets.UTF_8.decode(buffer.read()).toString());
			if(!parse.isSuccessful()) {
				for(Problem problem : parse.getProblems()) {
					System.err.println(problem.getVerboseMessage());
				}
			} else {
				CompilationUnit unit = parse.getResult().orElseThrow();
				var copy = new NodeList<>(unit.getImports());
				transformer.transform(unit);
				unit.setImports(copy);
				buffer.writeToOutput(ByteBuffer.wrap(unit.toString().getBytes(StandardCharsets.UTF_8)));
			}
			return ProcessResult.HANDLED;
		});

		for(TransformDependency.Input<?> input : inputs) {
			input.appendInputs(builder);
		}
	}

	@Override
	public void hash(Hasher hasher) {
		AmalgIO.hash(hasher, this.resolved);
	}
}
