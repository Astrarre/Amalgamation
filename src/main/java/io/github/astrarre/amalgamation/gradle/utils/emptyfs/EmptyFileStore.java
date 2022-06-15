package io.github.astrarre.amalgamation.gradle.utils.emptyfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class EmptyFileStore extends FileStore {
	@Override
	public String name() {
		return "root";
	}
	
	@Override
	public String type() {
		return "empty";
	}
	
	@Override
	public boolean isReadOnly() {
		return true;
	}
	
	@Override
	public long getTotalSpace() throws IOException {
		return 0;
	}
	
	@Override
	public long getUsableSpace() throws IOException {
		return 0;
	}
	
	@Override
	public long getUnallocatedSpace() throws IOException {
		return 0;
	}
	
	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return false;
	}
	
	@Override
	public boolean supportsFileAttributeView(String name) {
		return false;
	}
	
	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return null;
	}
	
	@Override
	public Object getAttribute(String attribute) throws IOException {
		return null;
	}
}
