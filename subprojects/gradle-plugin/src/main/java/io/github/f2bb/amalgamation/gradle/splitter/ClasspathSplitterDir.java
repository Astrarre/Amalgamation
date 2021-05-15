package io.github.f2bb.amalgamation.gradle.splitter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.impl.AccessSplitter;
import io.github.astrarre.splitter.impl.ClassSplitter;
import io.github.astrarre.splitter.impl.HeaderSplitter;
import io.github.astrarre.splitter.impl.InnerClassAttributeSplitter;
import io.github.astrarre.splitter.impl.InterfaceSplitter;
import io.github.astrarre.splitter.impl.SignatureSplitter;
import io.github.astrarre.splitter.impl.Splitters;
import io.github.astrarre.splitter.impl.SuperclassSplitter;
import io.github.f2bb.amalgamation.gradle.dependencies.MergerDependency;
import io.github.f2bb.amalgamation.gradle.util.CachedFile;
import io.github.f2bb.amalgamation.gradle.util.func.UnsafeConsumer;
import io.github.f2bb.amalgamation.platform.merger.PlatformMerger;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
// todo this is bad, make one CachedFile per classpath thing
public class ClasspathSplitterDir extends CachedFile<String> {
	protected final Project project;
	protected final List<Path> input;
	protected final List<String> toSplit;
	public ClasspathSplitterDir(Path file, Project project, List<Path> input, List<String> split) {
		super(file, (Class)List.class);
		this.project = project;
		this.toSplit = split;
		this.input = input;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		Hasher hasher = Hashing.sha256().newHasher();
		this.toSplit.forEach(hasher::putUnencodedChars);
		for (Path input : this.input) {
			hasher.putLong(Files.getLastModifiedTime(input).toMillis());
			hasher.putUnencodedChars(input.getFileName().toString());
		}

		String hash = hasher.hash().toString();
		if(hash.equals(currentData)) {
			return null;
		}

		List<Splitter> splitters = Splitters.defaults(null);

		boolean areDirectories = this.input.size() > 1 || this.input.stream().anyMatch(Files::isDirectory);
		if (areDirectories) {
			this.project.getLogger().info("Assuming " + this.input + " is project specific directories!");
		}

		if(areDirectories) {
			this.stripTo(splitters, path, this.input);
		} else {
			Path input = this.input.get(0);
			this.stripTo(splitters, path, Collections.singleton(input));
		}

		return hash;
	}

	protected void stripTo(List<Splitter> splitters, Path jarPath, Iterable<Path> fromPath) throws URISyntaxException, IOException {
		Files.createDirectories(jarPath.getParent());
		Files.deleteIfExists(jarPath);
		try(FileSystem writeSystem = FileSystems.newFileSystem(new URI("jar:" + jarPath.toUri()), MergerDependency.CREATE_ZIP)) {
			for (Path path : fromPath) {
				try(FileSystem readSystem = FileSystems.newFileSystem(path, null)) {
					for (Path root : readSystem.getRootDirectories()) {
						this.stripTo(splitters, writeSystem, root);
					}
				} catch (ProviderNotFoundException e) {
					this.stripTo(splitters, writeSystem, path);
				}
			}
		}
	}

	protected void stripTo(List<Splitter> splitters, FileSystem writeSystem, Path root) throws IOException {
		Files.walk(root).forEach(((UnsafeConsumer<Path>) file -> {
			for (Path writeRoot : writeSystem.getRootDirectories()) {
				Path dest = writeRoot.resolve(root.relativize(file).toString());
				if (!(Files.exists(dest) || Files.isDirectory(file))) {
					byte[] output;

					if(!dest.toString().endsWith(".class")) {
						output = Files.readAllBytes(file);
					} else {
						ClassNode from = PlatformMerger.read(Files.readAllBytes(file));
						ClassNode splitted = new ClassNode();
						PlatformId platformId = new PlatformId(this.toSplit);
						for (Splitter splitter : splitters) {
							if(splitter.split(from, platformId, splitted)) {
								return;
							}
						}
						ClassWriter writer = new ClassWriter(0);
						splitted.accept(writer);
						output = writer.toByteArray();
					}

					Files.createDirectories(dest.getParent());
					Files.write(dest, output);
				} else if(Files.exists(dest) && !Files.isDirectory(dest)) {
					System.err.println(dest + " is duplicated!");
				}
			}
		})::acceptFailException);
	}
}
