package io.github.astrarre.amalgamation.gradle.platform.merger;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.utils.AmalgamationIO;

public class MergerFile implements Closeable {
	final List<Path> input = new ArrayList<>(), classpath = new ArrayList<>();
	final List<Closeable> toClose = new ArrayList<>();
	Path resourcesOutput;

	public MergerFile addInputZip(Path file) {
		try {
			FileSystem outputSystem = FileSystems.newFileSystem(file, null);
			for (Path directory : outputSystem.getRootDirectories()) {
				this.addInputDirectory(directory);
			}
			this.toClose.add(outputSystem);
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public MergerFile addInputDirectory(Path directory) {
		this.input.add(directory);
		return this;
	}

	public MergerFile addClasspathZip(Path file) {
		try {
			FileSystem outputSystem = FileSystems.newFileSystem(file, null);
			for (Path directory : outputSystem.getRootDirectories()) {
				this.addClasspathDirectory(directory);
			}
			this.toClose.add(outputSystem);
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public MergerFile addClasspathDirectory(Path directory) {
		this.classpath.add(directory);
		return this;
	}

	public MergerFile setResourcesOutputZip(Path file) {
		if(this.resourcesOutput != null) throw new IllegalStateException("Cannot set resources twice!");
		try {
			FileSystem outputSystem = FileSystems.newFileSystem(new URI("jar:" + file.toUri()), AmalgamationIO.CREATE_ZIP);
			this.setResourcesOutputDirectory(Iterables.getFirst(outputSystem.getRootDirectories(), null));
			this.toClose.add(outputSystem);
			return this;
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public MergerFile setResourcesOutputDirectory(Path directory) {
		if(this.resourcesOutput != null) throw new IllegalStateException("Cannot set resources twice!");
		if(directory == null) throw new IllegalArgumentException("directory cannot be null!");
		this.resourcesOutput = directory;
		return this;
	}

	@Override
	public void close() throws IOException {
		for (Closeable closeable : this.toClose) {
			closeable.close();
		}
	}
}
