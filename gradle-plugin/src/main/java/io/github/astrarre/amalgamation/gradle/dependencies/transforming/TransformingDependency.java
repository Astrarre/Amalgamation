package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.filtr.Filters;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

// todo source transformations
@SuppressWarnings("UnstableApiUsage")
public class TransformingDependency extends ZipProcessDependency {
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
		this.dependencies.add(this.of(depNotation));
	}

	public void transform(Object depNotation, Closure<ModuleDependency> config) {
		this.dependencies.add(this.of(depNotation, config));
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
	public void hashInputs(Hasher hasher) throws IOException {
		for(Transformer transformer : this.transformers) {
			transformer.hash(hasher);
		}
		for(Dependency dependency : this.dependencies) {
			this.hashDep(hasher, dependency);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return AmalgIO.projectCache(this.project).resolve(this.name).resolve(AmalgIO.b64(hash));
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		// todo add isOutdated support
		for(Dependency dependency : this.dependencies) {
			resolver.apply(dependency, p -> {
				Hasher hasher = HASHING.newHasher();
				Path path = p.getVirtualPath();
				hasher.putString(path.toAbsolutePath().toString(), StandardCharsets.UTF_8);
				this.transformers.forEach(t -> t.hash(hasher));
				String hash = AmalgIO.hash(hasher);
				Path resolve = AmalgIO.projectCache(this.project).resolve(this.name).resolve(AmalgIO.insertName(path, hash));
				return Filters.from(p, resolve);
			});
			process.setEntryProcessor(b -> {
				String name = b.path();
				if(name.endsWith(".class")) {
					ByteBuffer buffer1 = b.read();
					ClassReader reader = new ClassReader(buffer1.array(), buffer1.arrayOffset(), buffer1.position());
					ClassNode node = new ClassNode();
					reader.accept(node, 0);
					int flags = 0;
					for(Transformer transformer : TransformingDependency.this.transformers) {
						node = transformer.apply(node);
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
		}
	}
}
