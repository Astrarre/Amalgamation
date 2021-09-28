package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.AbstractSelfResolvingDependency;
import io.github.astrarre.amalgamation.gradle.files.ZipProcessCachedFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class TransformingDependency extends AbstractSelfResolvingDependency {
	public final List<Dependency> dependencies;
	public final List<Transformer> transformers;

	public TransformingDependency(Project project, String name) {
		super(project, "io.github.astrarre.amalgamation", name, "0.0.0");
		this.dependencies = new ArrayList<>();
		this.transformers = new ArrayList<>();
	}

	public TransformingDependency(Project project,
			String group,
			String name,
			String version,
			List<Dependency> dependencies,
			List<Transformer> transformer) {
		super(project, group, name, version);
		this.dependencies = dependencies;
		this.transformers = transformer;
	}

	public void transform(Object depNotation) {
		this.dependencies.add(this.project.getDependencies().create(depNotation));
	}

	public void transform(Object depNotation, Closure<ModuleDependency> config) {
		this.dependencies.add(this.project.getDependencies().create(depNotation, config));
	}

	public void transformer(Transformer transformer) {
		this.transformers.add(transformer);
	}

	public void accessWidener(Object depNotation) throws IOException {
		this.transformer(new AWTransformer(AmalgIO.resolve(this.project, depNotation)));
	}

	@Override
	public Dependency copy() {
		return new TransformingDependency(
				this.project,
				this.group,
				this.name,
				this.version,
				new ArrayList<>(this.dependencies),
				new ArrayList<>(this.transformers));
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		List<Path> transformed = new ArrayList<>();
		for(File file : this.resolve(this.dependencies)) {
			Hasher hasher = Hashing.sha256().newHasher();
			AmalgIO.hash(hasher, file);
			this.transformers.forEach(t -> t.hash(hasher));
			String hash = Base64.getUrlEncoder().encodeToString(hasher.hash().asBytes());
			Path path = AmalgIO.projectCache(this.project).resolve(this.name).resolve(hash + "_" + file.getName());
			TransformingCachedFile cached = new TransformingCachedFile(path, hash, file.toPath());
			transformed.add(cached.getOutput());
		}
		return transformed;
	}

	public class TransformingCachedFile extends ZipProcessCachedFile {
		public final String hash;
		public final Path inputFile;
		public TransformingCachedFile(Path file, String hash, Path inputFile) {
			super(file, TransformingDependency.this.project);
			this.hash = hash;
			this.inputFile = inputFile;
		}

		@Override
		public void hashInputs(Hasher hasher) {
			hasher.putUnencodedChars(this.hash);
		}

		@Override
		public void init(ZipProcessBuilder process, Path outputFile) throws IOException {
			process.setEntryProcessor(b -> {
				String name = b.path();
				if(name.endsWith(".class")) {
					ByteBuffer buffer1 = b.read();
					ClassReader reader = new ClassReader(buffer1.array(), buffer1.arrayOffset(), buffer1.position());
					ClassNode node = new ClassNode();
					reader.accept(node, 0);
					int flags = 0;
					for(Transformer transformer : TransformingDependency.this.transformers) {
						transformer.apply(node);
						flags |= transformer.writerFlags();
					}
					ClassWriter writer = new ClassWriter(flags);
					node.accept(writer);
					b.writeToOutput(ByteBuffer.wrap(writer.toByteArray()));
				} else {
					b.copyToOutput();
				}
				return ProcessResult.HANDLED;
			});
			process.addZip(this.inputFile, outputFile);
		}
	}
}
