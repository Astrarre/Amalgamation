package io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;

import com.google.common.base.Objects;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.TransformDependency;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.internal.impldep.org.bouncycastle.util.Iterable;

@SuppressWarnings("unchecked")
public interface InputType<C extends TransformDependency.Transformer<?>, T> {
	BinaryOperator<Object> DEP = (o, o2) -> {
		var list = new ArrayList<>();
		append(o, list);
		append(o2, list);
		return list;
	};

	static <C extends TransformDependency.Transformer<?>, T> InputType<C, T> of(AmalgDirs location, BinaryOperator<T> collector, InputType<C, T> type) {
		return type.withLocation(location).withCollector(collector);
	}

	static <C extends TransformDependency.Transformer<?>, T> InputType<C, T> of(AmalgDirs location, InputType<C, T> type) {
		return type.withLocation(location);
	}

	static <C extends TransformDependency.Transformer<?>, T> InputType<C, T> of(AmalgDirs location, BinaryOperator<T> collector, InputType<C, T> type, String hint) {
		return type.withLocation(location).withCollector(collector).withHint(hint);
	}

	static <C extends TransformDependency.Transformer<?>, T> InputType<C, T> of(AmalgDirs location, InputType<C, T> type, String hint) {
		return type.withLocation(location).withHint(hint);
	}

	default AmalgDirs cacheLocation() {
		return null;
	}

	default BinaryOperator<T> collector() {
		return (t, t2) -> {
			if(Objects.equal(t, t2)) {
				return t;
			} else {
				throw new UnsupportedOperationException("cannot combine " + t + " and " + t2 + " when in doubt, use TransformConfiguration#inputAll");
			}
		};
	}

	/**
	 * @param representation null if {@link #cacheLocation()} is {@link null}
	 * @param input the input dependency
	 * @return a value
	 */
	T apply(Project project, C transformer, Dependency representation, Dependency input);

	default InputType<C, T> withLocation(AmalgDirs location) {
		return new Impl<>(location, this, this.collector(), this.hint());
	}

	default InputType<C, T> withCollector(BinaryOperator<T> collector) {
		return new Impl<>(this.cacheLocation(), this, collector, this.hint());
	}

	default InputType<C, T> withHint(String hint) {
		return new Impl<>(this.cacheLocation(), this, this.collector(), hint);
	}

	default String hint() {
		return this.getClass().getSimpleName();
	}

	record Impl<C extends TransformDependency.Transformer<?>, T>(AmalgDirs cacheLocation, InputType<C, T> function, BinaryOperator<T> collector, String hint) implements InputType<C, T> {
		@Override
		public T apply(Project project, C transformer, Dependency representation, Dependency input) {
			return this.function.apply(project, transformer, representation, input);
		}

		@Override
		public AmalgDirs cacheLocation() {
			return this.cacheLocation;
		}

		@Override
		public InputType<C, T> withLocation(AmalgDirs location) {
			return new Impl<>(location, this.function, this.collector, this.hint);
		}

		@Override
		public InputType<C, T> withCollector(BinaryOperator<T> collector) {
			return new Impl<>(this.cacheLocation, this.function, collector, this.hint);
		}

		@Override
		public InputType<C, T> withHint(String hint) {
			return new Impl<>(this.cacheLocation, this.function, this.collector, hint);
		}

		@Override
		public String hint() {
			return this.hint;
		}
	}

	private static void append(Object o, List<Object> list) {
		if(o instanceof List<?> l) {
			list.addAll(l);
		} else if(o instanceof Dependency d) {
			list.add(d);
		} else if(o instanceof Iterable<?> iterable) {
			for(Object o1 : iterable) {
				list.add(o1);
			}
		} else if(o instanceof Object[] arr) {
			list.addAll(Arrays.asList(arr));
		} else {
			throw new UnsupportedOperationException("Unrecognized type " + o.getClass() + " obj:" + o);
		}
	}
}
