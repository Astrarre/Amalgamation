package io.github.astrarre.amalgamation.gradle.utils.func;

public interface UnsafeSupplier<T> {
	T get() throws Throwable;

	default T acceptFailException() {
		try {
			return this.get();
		} catch (Throwable tr) {
			throw new RuntimeException(tr);
		}
	}
}
