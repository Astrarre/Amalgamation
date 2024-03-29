package io.github.astrarre.amalgamation.gradle.utils.func;

import java.util.function.Consumer;

import net.devtech.zipio.impl.util.U;

public interface UCons<T> extends Consumer<T> {
	static <T> UCons<T> of(UCons<T> t) {
		return t;
	}

	@Override
	default void accept(T t) {
		try {
			this.acceptFailException(t);
		} catch (Throwable tr) {
			throw U.rethrow(tr);
		}
	}

	void acceptFailException(T t) throws Throwable;
}
