package io.github.f2bb.amalgamation.gradle.util.func;

public interface UnsafeConsumer<T> {
	void accept(T t) throws Throwable;

	default void acceptFailException(T t) {
		try {
			this.accept(t);
		} catch (Throwable tr) {
			throw new RuntimeException(tr);
		}
	}
}
