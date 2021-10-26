package io.github.astrarre.amalgamation.gradle.dependencies.transform;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.ApiStatus;

public interface TransformConfiguration {
	@ApiStatus.NonExtendable
	<T> T input(InputType<T> type, Object depNotation);

	@ApiStatus.NonExtendable
	<T> T input(InputType<T> type, Object depNotation, Closure<ModuleDependency> dep);
}
