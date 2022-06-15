package io.github.astrarre.amalgamation.gradle.utils.emptyfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyFileSystemProvider extends FileSystemProvider {
	private static final URI DEFAULT = null;
	public static final EmptyFileSystemProvider INSTANCE = new EmptyFileSystemProvider();
	public static final EmptyFileSystem EMPTY = INSTANCE.newFileSystem(DEFAULT, Map.of());
	
	final Map<URI, EmptyFileSystem> system = new ConcurrentHashMap<>();
	
	
	@Override
	public String getScheme() {
		return "empty";
	}
	
	@Override
	public EmptyFileSystem newFileSystem(@Nullable URI uri, Map<String, ?> env) {
		EmptyFileSystem value = new EmptyFileSystem(this, uri);
		Objects.requireNonNull(this.system.putIfAbsent(uri, value), "file system already exists for " + uri);
		return value;
	}
	
	@Override
	public FileSystem getFileSystem(URI uri) {
		return this.system.get(uri);
	}
	
	@NotNull
	@Override
	public Path getPath(@NotNull URI uri) {
		return new EmptyPath(this.system.computeIfAbsent(uri, u -> new EmptyFileSystem(this, uri)), uri.getPath());
	}
	
	@Override
	public SeekableByteChannel newByteChannel(
			Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		throw new FileNotFoundException(path.toString());
	}
	
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
		return new DirectoryStream<>() {
			@Override
			public Iterator<Path> iterator() {
				return new Iterator<>() {
					@Override
					public boolean hasNext() {
						return false;
					}
					
					@Override
					public Path next() {
						return null;
					}
				};
			}
			
			@Override
			public void close() throws IOException {
			
			}
		};
	}
	
	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		throw new ReadOnlyFileSystemException();
	}
	
	@Override
	public void delete(Path path) throws IOException {
		throw new ReadOnlyFileSystemException();
	}
	
	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		throw new ReadOnlyFileSystemException();
	}
	
	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		throw new ReadOnlyFileSystemException();
	}
	
	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return path.compareTo(path2) == 0;
	}
	
	@Override
	public boolean isHidden(Path path) throws IOException {
		return false;
	}
	
	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return new EmptyFileStore();
	}
	
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
	}
	
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		return null;
	}
	
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
		return null;
	}
	
	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		return Map.of();
	}
	
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}
}
