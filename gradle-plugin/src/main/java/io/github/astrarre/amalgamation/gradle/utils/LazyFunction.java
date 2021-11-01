package io.github.astrarre.amalgamation.gradle.utils;

import java.util.function.Function;

public class LazyFunction<A, B> implements Function<A, B> {
	private Function<A, B> function;
	private B value;

	public LazyFunction(Function<A, B> function) {
		this.function = function;
	}

	@Override
	public B apply(A a) {
		Function<A, B> function = this.function;
		if(function != null) {
			this.function = null;
			return this.value = function.apply(a);
		} else {
			return this.value;
		}
	}
}
