package io.github.astrarre.amalgamation.gradle.utils.emptyfs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class EmptyPath implements Path {
	final EmptyFileSystem fs;
	final String path;
	EmptyPath parent;
	
	public EmptyPath(EmptyFileSystem system, String path) {
		this.fs = system;
		this.path = path;
	}
	
	@NotNull
	@Override
	public FileSystem getFileSystem() {
		return this.fs;
	}
	
	@Override
	public boolean isAbsolute() {
		return !this.path.isEmpty() && this.path.charAt(0) == '/';
	}
	
	@Override
	public Path getRoot() {
		return this.fs.root;
	}
	
	@Override
	public Path getFileName() {
		String name = this.path;
		int from = name.lastIndexOf('/') + 1;
		if(from == 0) {
			return this;
		} else {
			return new EmptyPath(this.fs, name.substring(from));
		}
	}
	
	@Override
	public Path getParent() {
		String name = this.path;
		int from = name.lastIndexOf('/') + 1;
		if(from == 0) {
			return null;
		} else {
			EmptyPath parent = this.parent;
			if(parent == null) {
				this.parent = parent = new EmptyPath(this.fs, name.substring(0, from));
			}
			return parent;
		}
	}
	
	@Override
	public int getNameCount() {
		String path = this.path;
		int count = 0;
		// skip first and last char cus they may be abs paths or a directory
		for(int i = 1; i < path.length() - 1; i++) {
			if(path.charAt(i) == '/') {
				count++;
			}
		}
		return count;
	}
	
	@NotNull
	@Override
	public Path getName(int index) {
		int curr = 0;
		for(int i = 0; i < index; i++) {
			curr = this.path.indexOf('/', curr) + 1;
			if(curr == 1) {
				i--;
			}
		}
		if(index == 0 && this.isAbsolute()) {
			curr = 1;
		}
		if(curr == 0 && index > 0) {
			throw new IllegalArgumentException("index cannot be >= nameCount!");
		}
		int end = this.path.indexOf('/', curr);
		if(end == -1) {
			end = this.path.length();
		}
		return new EmptyPath(this.fs, this.path.substring(curr, end));
	}
	
	@NotNull
	@Override
	public Path subpath(int beginIndex, int endIndex) {
		int start = 0;
		for(int i = 0; i < beginIndex; i++) {
			start = this.path.indexOf('/', start) + 1;
			if(start == 1) {
				i--;
			}
		}
		if(start == 0 && beginIndex > 0) {
			throw new IllegalArgumentException("beginIndex cannot be >= nameCount!");
		}
		
		int end = start + 1;
		for(int i = 0; i < (endIndex - beginIndex); i++) {
			end = this.path.indexOf('/', end) + 1;
		}
		if(end == 1 && endIndex != beginIndex) {
			throw new IllegalArgumentException("beginIndex cannot be >= nameCount!");
		} else if(end == 1) {
			end = start;
		}
		
		return new EmptyPath(this.fs, this.path.substring(start, end));
	}
	
	@Override
	public boolean startsWith(@NotNull Path other) {
		int off;
		if(this.isAbsolute() && !other.isAbsolute()) {
			off = 1;
		} else {
			off = 0;
		}
		return this.path.startsWith(((EmptyPath) other).path, off);
	}
	
	@Override
	public boolean endsWith(@NotNull Path other) {
		return this.path.endsWith(((EmptyPath) other).path);
	}
	
	@NotNull
	@Override
	public Path normalize() {
		return this;
	}
	
	@NotNull
	@Override
	public Path resolve(@NotNull Path other) {
		String path = ((EmptyPath) other).path;
		StringBuilder combined = new StringBuilder(path.length() + this.path.length() + 1);
		combined.append(this.path);
		if(!this.path.endsWith("/")) {
			combined.append('/');
		}
		combined.append(path);
		return new EmptyPath(this.fs, combined.toString());
	}
	
	@NotNull
	@Override
	public Path relativize(@NotNull Path other) {
		String path = ((EmptyPath)other).path;
		if(path.startsWith(this.path)) {
			return new EmptyPath(this.fs, path.substring(this.path.length()));
		}
		throw new IllegalArgumentException();
	}
	
	@NotNull
	@Override
	public URI toUri() {
		try {
			return new URI("empty", this.path, null);
		} catch(URISyntaxException e) {
			throw Err.rethrow(e);
		}
	}
	
	@NotNull
	@Override
	public Path toAbsolutePath() {
		if(this.isAbsolute()) return this;
		return new EmptyPath(this.fs, "/" + this.path);
	}
	
	@NotNull
	@Override
	public Path toRealPath(@NotNull LinkOption... options) throws IOException {
		return this.toAbsolutePath();
	}
	
	@NotNull
	@Override
	public WatchKey register(
			@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
		return new WatchKey() {
			@Override
			public boolean isValid() {
				return false;
			}
			
			@Override
			public List<WatchEvent<?>> pollEvents() {
				return List.of();
			}
			
			@Override
			public boolean reset() {
				return false;
			}
			
			@Override
			public void cancel() {
			}
			
			@Override
			public Watchable watchable() {
				return EmptyPath.this;
			}
		};
	}
	
	@Override
	public int compareTo(@NotNull Path other) {
		return this.path.compareTo(((EmptyPath)other).path);
	}
	
	@Override
	public String toString() {
		return this.path;
	}
}
