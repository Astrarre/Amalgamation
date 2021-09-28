package io.github.astrarre.amalgamation.gradle.utils.func;

public interface UCons<T> {
	static void run(UCons<?> consumer) {
		consumer.acceptFailException(null);
	}

	void accept(T t) throws Throwable;

	default void acceptFailException(T t) {
		try {
			this.accept(t);
		} catch (Throwable tr) {
			throw new RuntimeException(tr);
		}
	}
}
