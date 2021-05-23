package io.github.astrarre.amalgamation.gradle.utils.func;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

public interface UnsafeIterable<T> extends Iterable<T> {
	static UnsafeIterable<Path> walkPath(Path path) {
		return () -> Files.walk(path).filter(Files::isRegularFile).iterator();
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
