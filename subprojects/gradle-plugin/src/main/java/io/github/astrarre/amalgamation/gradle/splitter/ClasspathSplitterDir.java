package io.github.astrarre.amalgamation.gradle.splitter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Collections;
import java.util.List;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.utils.CachedFile;
import io.github.astrarre.amalgamation.utils.Clock;
import io.github.astrarre.amalgamation.utils.func.UnsafeConsumer;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.merger.Mergers;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.impl.Splitters;
import org.checkerframework.checker.units.qual.C;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

// todo this is bad, make one CachedFile per file
public class ClasspathSplitterDir extends CachedFile<String> {
	protected final Project project;
	protected final List<Path> input;
	protected final List<String> toSplit;

	public ClasspathSplitterDir(Path file, Project project, List<Path> input, List<String> split) {
		super(file, String.class);
		this.project = project;
		this.toSplit = split;
		this.input = input;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		try (Clock clock = new Clock("splitting " + this.input + " took %sms", this.project.getLogger())) {
			Hasher hasher = Hashing.sha256().newHasher();
			this.toSplit.forEach(hasher::putUnencodedChars);
			for (Path input : this.input) {
				hasher.putLong(Files.getLastModifiedTime(input).toMillis());
				hasher.putUnencodedChars(input.getFileName().toString());
			}

			String hash = hasher.hash().toString();
			if (hash.equals(currentData)) {
				clock.message = "Cache validation / download for " + this.input + " took %sms";
				return null;
			}

			List<Splitter> splitters = Splitters.defaults(null);

			boolean areDirectories = this.input.size() > 1 || this.input.stream().anyMatch(Files::isDirectory);
			if (areDirectories) {
				this.project.getLogger().info("Assuming " + this.input + " is project specific directories!");
			}

			if (areDirectories) {
				this.stripTo(splitters, path, this.input);
			} else {
				Path input = this.input.get(0);
				this.stripTo(splitters, path, Collections.singleton(input));
			}

			return hash;
		}
	}

	protected void stripTo(List<Splitter> splitters, Path jarPath, Iterable<Path> fromPath) throws URISyntaxException, IOException {
		Files.createDirectories(jarPath.getParent());
		Files.deleteIfExists(jarPath);
		FileSystem system = null;
		for (Path path : fromPath) {
			this.project.getLogger().lifecycle(path+"");
			try (FileSystem readSystem = FileSystems.newFileSystem(path, null)) {
				for (Path root : readSystem.getRootDirectories()) {
					if(system == null) system = FileSystems.newFileSystem(new URI("jar:" + jarPath.toUri()), Mergers.CREATE_ZIP);
					this.stripTo(splitters, system, root);
				}
			} catch (ProviderNotFoundException e) {
				if(system == null) system = FileSystems.newFileSystem(new URI("jar:" + jarPath.toUri()), Mergers.CREATE_ZIP);
				this.stripTo(splitters, system, path);
			}
		}
		if(system != null) system.close();
	}

	protected void stripTo(List<Splitter> splitters, FileSystem writeSystem, Path root) throws IOException {
		Files.walk(root).forEach(((UnsafeConsumer<Path>) file -> {
			for (Path writeRoot : writeSystem.getRootDirectories()) {
				Path dest = writeRoot.resolve(root.relativize(file).toString());
				if (!(Files.exists(dest) || Files.isDirectory(file))) {
					byte[] output;

					if (!dest.toString().endsWith(".class")) {
						output = Files.readAllBytes(file);
					} else {
						ClassNode from = from(Files.readAllBytes(file));
						ClassNode splitted = new ClassNode();
						PlatformId platformId = new PlatformId(this.toSplit);
						for (Splitter splitter : splitters) {
							if (splitter.split(from, platformId, splitted)) {
								return;
							}
						}
						ClassWriter writer = new ClassWriter(0);
						splitted.accept(writer);
						output = writer.toByteArray();
					}

					Files.createDirectories(dest.getParent());
					Files.write(dest, output);
				} else if (Files.exists(dest) && !Files.isDirectory(dest)) {
					System.err.println(dest + " is duplicated!");
				}
			}
		})::acceptFailException);
	}

	public static ClassNode from(byte[] bytes) {
		ClassReader reader = new ClassReader(bytes);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);
		return node;
	}
}
