package io.github.astrarre.amalgamation.gradle.dependencies.transform.aw;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.TransformConfiguration;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

public interface AccessWidenerHelper extends TransformConfiguration<AccessWidenerHelper, AccessWidenerTransform> {
	InputType<AccessWidenerTransform, Object> INPUT = InputType.of(AmalgDirs.PROJECT, InputType.DEP, (p, t, r, i) -> r, "accessWidener");
	InputType<AccessWidenerTransform, Void> FILE = InputType.of(null, (p, t, r, i) -> t.applyFile(p, i), "accessWidener");

	default InputType<AccessWidenerTransform, Object> input() {
		return INPUT;
	}

	default InputType<AccessWidenerTransform, Void> aw() {
		return FILE;
	}

	default Object input(Object depNotation) {
		return this.input(INPUT, depNotation);
	}

	default Object input(Object depNotation, Closure<ModuleDependency> configure) {
		return this.input(INPUT, depNotation, configure);
	}

	default void aw(Object depNotation) {
		this.input(FILE, depNotation);
	}

	default void aw(Object depNotation, Closure<ModuleDependency> configure) {
		this.input(FILE, depNotation, configure);
	}
}
