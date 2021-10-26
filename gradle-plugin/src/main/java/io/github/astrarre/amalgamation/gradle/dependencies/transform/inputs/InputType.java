package io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs;

public interface InputType<T> {
	enum CacheLocation {
		PROJECT,
		GLOBAL,
		NONE;

		public boolean isGlobal() {
			return this == GLOBAL;
		}
	}

	CacheLocation getCacheLocation();

	default String hint() {
		return this.getClass().getSimpleName();
	}
}
