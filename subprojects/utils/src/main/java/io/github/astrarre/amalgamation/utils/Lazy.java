package io.github.astrarre.amalgamation.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A superior Lazy class to Mojang's, works with null values
 */
public final class Lazy<T> implements Supplier<T>, Callable<T> {
	public static final Lazy<?> EMPTY = new Lazy<>((Object) null);

	@Nullable
	private Supplier<T> supplier;
	private T instance;

	public static <T> Lazy<T> of(Supplier<T> supplier) {
		return new Lazy<>(supplier);
	}

	public static <T> Lazy<T> of(T value) {
		if(value == null) {
			return empty();
		}
		return new Lazy<>(value);
	}

	/**
	 * @return a pre-evaluated lazy for `null`
	 */
	public static <T> Lazy<T> empty() {
		return (Lazy<T>) EMPTY;
	}

	/**
	 * @return if `value` is null, returns a new lazy with the given function, else uses the value for a pre-evaluated lazy
	 */
	public static <T> Lazy<T> or(@Nullable T value, Supplier<T> getter) {
		if(value == null) {
			return new Lazy<>(getter);
		} else {
			return of(value);
		}
	}

	/**
	 * @see #of(Object)
	 */
	public Lazy(T instance) {
		this.supplier = null;
		this.instance = instance;
	}

	public Lazy(@NotNull Supplier<T> supplier) {
		this.supplier = Objects.requireNonNull(supplier, "Supplier may not be null");
	}

	@Override
	public T get() {
		T instance = this.instance;
		Supplier<T> supplier = this.supplier;
		if(supplier != null) {
			instance = Objects.requireNonNull(supplier.get(), "Lazy supplier may not return null!");
			this.instance = instance;
			this.supplier = null;
		}
		return instance;
	}

	@Contract("_ -> new")
	public <K> Lazy<K> map(Function<T, K> mapper) {
		T instance = this.instance;
		if(this.supplier != null) {
			return new Lazy<>(() -> mapper.apply(this.get()));
		} else {
			return new Lazy<>(() -> mapper.apply(instance));
		}
	}


	/**
	 * Returns the instance returned by the supplier if the Lazy has already been evaluated. Returns null otherwise
	 */
	@Nullable
	public T getRaw() {
		return this.instance;
	}

	/**
	 * @return if the Lazy is evaluated, return the instance, else evaluates the given supplier
	 */
	public T getRawOrGet(Supplier<T> supplier) {
		if(this.supplier == null) {
			return this.instance;
		} else {
			return supplier.get();
		}
	}

	/**
	 * @return if the Lazy is evaluated, return the instance, else return val
	 */
	public T getRaw(T val) {
		if(this.supplier == null) {
			return this.instance;
		} else {
			return val;
		}
	}

	/**
	 * @return true if the supplier for this Lazy has been called
	 */
	public boolean hasEvaluated() {
		return this.supplier == null;
	}

	public State getState() {
		if(this.supplier != null) {
			return State.UNEVALUATED;
		} else if(this.instance != null) {
			return State.PRESENT;
		} else {
			return State.NULL;
		}
	}

	public Optional<T> raw() {
		return Optional.ofNullable(this.instance);
	}

	@Override
	public T call() throws Exception {
		return this.get();
	}

	public enum State {
		/**
		 * the lazy contains a non-null value
		 */
		PRESENT,
		/**
		 * the lazy was evaluated, and contains a null value
		 */
		NULL,
		/**
		 * the lazy has not been evaluated
		 */
		UNEVALUATED
	}
}
