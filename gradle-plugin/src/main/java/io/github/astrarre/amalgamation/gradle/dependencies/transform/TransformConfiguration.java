package io.github.astrarre.amalgamation.gradle.dependencies.transform;

import java.util.List;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.ApiStatus;

public interface TransformConfiguration<Self extends TransformConfiguration<Self, D>, D extends TransformDependency.Transformer<Self>> {
	@ApiStatus.NonExtendable
	<T> List<T> inputAll(InputType<D, T> type, List<?> list);

	@ApiStatus.NonExtendable
	<T> T input(InputType<D, T> type, Object depNotation);

	@ApiStatus.NonExtendable
	<T> T input(InputType<D, T> type, Object depNotation, Closure<ModuleDependency> dep);

	D getTransformer();

	Project getProject();

	// todo unify RemapDependency and this
}
