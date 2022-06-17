package io.github.astrarre.amalgamation.gradle.utils.zip;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import net.devtech.betterzipfs.ZipFS;
import net.devtech.betterzipfs.impl.ZipFSProvider;

public final class FSRef implements AutoCloseable {
	private final FileSystem value;
	Cleaner.Cleanable action;
	
	public FSRef(FileSystem value) {
		this.value = value;
	}
	
	// todo nested walk to optimize for createDirectories
	
	public Stream<Path> directorizingStream(FSRef output) throws IOException {
		return ZipFSProvider.walk(this.fs()).filter(p -> {
			if(Files.isDirectory(p)) {
				try {
					Files.createDirectories(output.getPath(p.toString()));
					return false;
				} catch(IOException e) {
					throw Err.rethrow(e);
				}
			} else {
				return true;
			}
		});
	}
	
	public Stream<Path> walk() throws IOException { // binary merge
		return ZipFS.unorderedFastStream(this.value);
	}
	
	public void flush() {
		ZipFS.flush(this.value, false);
	}
	
	@Override
	public void close() throws Exception {
		this.flush();
	}
	
	public FileSystem fs() {
		return value;
	}
	
	private static final String[] EMPTY = {};
	public Path getPath(String path) {
		return this.fs().getPath(path, EMPTY);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		var that = (FSRef) obj;
		return Objects.equals(this.value, that.value);
	}
	
	@Override
	public String toString() {
		return "FSRef[" + this.value + ']';
	}
	
	public interface DirectoryWalker {
		void enterDir(Path path);
		
		void exitDir(Path path);
	}
	
}
