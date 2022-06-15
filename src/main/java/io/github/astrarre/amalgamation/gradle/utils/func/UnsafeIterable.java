package io.github.astrarre.amalgamation.gradle.utils.func;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.google.common.collect.Streams;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import org.jetbrains.annotations.NotNull;

public interface UnsafeIterable<T> extends Iterable<T> {
	static UnsafeIterable<Path> walkFiles(Path path) {
		if(!Files.exists(path)) {
			return Collections::emptyIterator;
		}
		return () -> Files.walk(path).filter(Files::isRegularFile).iterator();
	}

	static UnsafeIterable<Path> walkAll(Path path) {
		if(!Files.exists(path)) {
			return Collections::emptyIterator;
		}
		return () -> Files.walk(path).sorted(Comparator.reverseOrder()).iterator();
	}

	static UnsafeIterable<Path> walkFiles(Iterable<Path> paths) {
		return () -> Streams.stream(paths).flatMap(p -> {
			try {
				return Files.walk(p);
			} catch(IOException e) {
				throw Err.rethrow(e);
			}
		}).filter(Files::isRegularFile).iterator();
	}

	@NotNull
	@Override
	default Iterator<T> iterator() {
		try {
			return this.iterUnsafe();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	Iterator<T> iterUnsafe() throws Throwable;
}
