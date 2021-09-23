package io.github.astrarre.amalgamation.gradle.dependencies.transforming;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.amalgamation.gradle.dependencies.AbstractSelfResolvingDependency;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;
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

	public void add(Object depNotation) {
		this.dependencies.add(this.project.getDependencies().create(depNotation));
	}

	public void addTransformer(Transformer transformer) {
		this.transformers.add(transformer);
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		List<Path> transformed = new ArrayList<>();
		for (File file : this.resolve(this.dependencies)) {
			TransformingCachedFile cached = new TransformingCachedFile(file.toPath(), AmalgIO.projectCache(this.project).resolve(this.name).resolve(file.getName()),
			                                                           this.transformers);
			transformed.add(cached.getPath());
		}
		return transformed;
	}

	@Override
	public Dependency copy() {
		return new TransformingDependency(this.project, this.group, this.name, this.version, new ArrayList<>(this.dependencies), new ArrayList<>(this.transformers));
	}

	public static class TransformingCachedFile extends CachedFile<String> {
		public final Path inputFile;
		public final List<Transformer> transformers;
		public TransformingCachedFile(Path file, Path inputFile, List<Transformer> transformers) {
			super(file, String.class);
			this.inputFile = inputFile;
			this.transformers = transformers;
		}

		@Override
		protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
			Files.createDirectories(path);
			try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(this.inputFile))); ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path.resolve("original.jar"))))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String name = entry.getName();
					boolean transforms = false;
					for (Transformer transformer : this.transformers) {
						if(transformer.processes(name)) {
							transforms = true;
							break;
						}
					}

					if(transforms) {
						byte[] read = AmalgIO.readAll(zis);
						for (Transformer transformer : this.transformers) {
							read = transformer.processResource(name, read);
						}
						if(name.endsWith(".class")) {
							ClassReader reader = new ClassReader(read);
							ClassNode node = new ClassNode();
							reader.accept(node, 0);
							int flags = 0;
							for (Transformer transformer : this.transformers) {
								transformer.apply(node);
								flags |= transformer.writerFlags();
							}
							ClassWriter writer = new ClassWriter(flags);
							node.accept(writer);
							zos.putNextEntry(new ZipEntry(entry.getName()));
							zos.write(writer.toByteArray());
							zos.closeEntry();
						}
					} else {
						zos.putNextEntry(new ZipEntry(entry.getName()));
						AmalgIO.copy(zis, zos);
						zos.closeEntry();
					}
				}
			}
			return null;
		}
	}
}
