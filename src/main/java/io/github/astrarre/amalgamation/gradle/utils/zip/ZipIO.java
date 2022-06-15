package io.github.astrarre.amalgamation.gradle.utils.zip;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.lang.ref.SoftReference;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.EmptyFileSystemProvider;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import net.devtech.betterzipfs.ZipFS;

public class ZipIO {
	static final Map<String, ?> CREATE_ZIP = Map.of("create", "true");
	static final Cleaner CLEANER = Cleaner.create();
	static final Map<Path, SoftReference<FSRef>> fsCache = new ConcurrentHashMap<>();
	
	/**
	 * Flushes the cache
	 */
	public static void nuclearOption() {
		Map<Path, SoftReference<FSRef>> copy = new ConcurrentHashMap<>(fsCache);
		fsCache.clear();
		for(SoftReference<FSRef> value : copy.values()) {
			FSRef ref = value.get();
			if(ref != null) {
				ref.action.clean();
			}
		}
	}
	
	public static FSRef createZip(Path path) throws IOException {
		return openZip(path, CREATE_ZIP, true);
	}
	
	public static FSRef readZip(Path path) throws IOException {
		return openZip(path, Map.of(), false);
	}
	
	public static FSRef openZip(Path path, Map<String, ?> settings, boolean write) throws IOException {
		if(!write && !Files.exists(path)) {
			System.err.println("[Amalgamation] [Warn] " + path + " does not exist!");
			return new FSRef(EmptyFileSystemProvider.EMPTY);
		}
		
		if(write) {
			AmalgIO.createParent(path);
		}
		
		SoftReference<FSRef> reference = fsCache.computeIfAbsent(path, path1 -> {
			try {
				FileSystem system = ZipFS.newFileSystem(path1, settings);
				FSRef ref = new FSRef(system);
				SoftReference<FSRef> soft = new SoftReference<>(ref);
				ref.action = CLEANER.register(ref, () -> {
					try {
						system.close();
						fsCache.remove(path1, soft);
						soft.clear();
					} catch(IOException e) {
						e.printStackTrace(); // print stack trace cus errors get swallowed in cleaners
					}
				});
				return soft;
			} catch(IOException e) {
				throw Err.rethrow(e);
			}
		});
		FSRef ref = reference.get();
		if(ref == null) {
			fsCache.remove(path, reference);
		}
		return ref;
	}
}
