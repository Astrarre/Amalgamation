package io.github.f2bb.amalgamation.gradle.util;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public class LazySet implements Set<File> {
	private final CompletableFuture<Set<File>> delegate;

	public LazySet(CompletableFuture<Set<File>> delegate) {
		this.delegate = delegate;
	}

	@Override
	public int size() {
		return this.delegate.join().size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.join().isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.delegate.join().contains(o);
	}

	@NotNull
	@Override
	public Iterator<File> iterator() {
		return this.delegate.join().iterator();
	}

	@NotNull
	@Override
	public Object[] toArray() {
		return this.delegate.join().toArray();
	}

	@NotNull
	@Override
	public <T> T[] toArray(@NotNull T[] a) {
		return this.delegate.join().toArray(a);
	}

	@Override
	public boolean add(File file) {
		return this.delegate.join().add(file);
	}

	@Override
	public boolean remove(Object o) {
		return this.delegate.join().remove(o);
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return this.delegate.join().containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends File> c) {
		return this.delegate.join().addAll(c);
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		return this.delegate.join().retainAll(c);
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		return this.delegate.join().removeAll(c);
	}

	@Override
	public void clear() {
		this.delegate.join().clear();
	}

	@Override
	public boolean equals(Object o) {
		return this.delegate.join().equals(o);
	}

	@Override
	public int hashCode() {
		return this.delegate.join().hashCode();
	}

	@Override
	public Spliterator<File> spliterator() {
		return this.delegate.join().spliterator();
	}

	@Override
	public boolean removeIf(Predicate<? super File> filter) {
		return this.delegate.join().removeIf(filter);
	}

	@Override
	public Stream<File> stream() {
		return this.delegate.join().stream();
	}

	@Override
	public Stream<File> parallelStream() {
		return this.delegate.join().parallelStream();
	}

	@Override
	public void forEach(Consumer<? super File> action) {
		this.delegate.join().forEach(action);
	}
}
