package io.github.astrarre.amalgamation.gradle.utils.func;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import net.devtech.zipio.impl.util.U;
import org.jetbrains.annotations.NotNull;

public interface UnsafeIterable<T> extends Iterable<T> {
	static UnsafeIterable<Path> walkFiles(Path path) {
		if(!Files.exists(path)) {
			return Collections::emptyIterator;
		}
		return () -> Files.walk(path).filter(Files::isRegularFile).iterator();
	}

	static UnsafeIterable<Path> walkFiles(Iterable<Path> paths) {
		return () -> Streams.stream(paths).flatMap(p -> {
			try {
				return Files.walk(p);
			} catch(IOException e) {
				throw U.rethrow(e);
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
