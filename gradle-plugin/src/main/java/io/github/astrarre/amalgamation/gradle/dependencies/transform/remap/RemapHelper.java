package io.github.astrarre.amalgamation.gradle.dependencies.transform.remap;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.cls.TRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.src.TrieHarderRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.TransformConfiguration;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

public interface RemapHelper extends TransformConfiguration<RemapHelper, RemapTransform> {
	InputType<RemapTransform, Object> INPUT_LOCAL = InputType.of(AmalgDirs.ROOT_PROJECT, InputType.DEP, (p, t, r, i) -> r, "remap");
	InputType<RemapTransform, Object> INPUT_GLOBAL = InputType.of(AmalgDirs.GLOBAL, InputType.DEP, (p, t, r, i) -> r, "remap");
	InputType<RemapTransform, Void> CLASSPATH = InputType.of(null, (p, t, r, i) -> null, "remap");

	/**
	 * sets the source remapper to the TrieHarder source remapper
	 */
	default void trieHarder() {
		this.setSrcRemapper(new TrieHarderRemapper());
	}

	/**
	 * sets the class remapper to fabricmc/tiny-remapper
	 */
	default void tinyRemapper() {
		this.setRemapper(new TRemapper());
	}

	default Object inputGlobal(Object dep) {
		return this.input(INPUT_GLOBAL, dep);
	}

	default Object inputLocal(Object dep) {
		return this.input(INPUT_LOCAL, dep);
	}

	default Object classpath(Object dep) {
		return this.input(CLASSPATH, dep);
	}

	default Object inputGlobal(Object dep, Closure<ModuleDependency> configure) {
		return this.input(INPUT_GLOBAL, dep, configure);
	}

	default Object inputLocal(Object dep, Closure<ModuleDependency> configure) {
		return this.input(INPUT_LOCAL, dep, configure);
	}

	default Object classpath(Object dep, Closure<ModuleDependency> configure) {
		return this.input(CLASSPATH, dep, configure);
	}

	default void mappings(Object dep, String from, String to) {
		MappingTarget mappings = new MappingTarget(this.getProject(), this.of(dep), from, to);
		this.getTransformer().addMappings(mappings);
	}

	default void mappings(MappingTarget mappings) {
		this.getTransformer().addMappings(mappings);
	}

	default void setRemapper(AmalgRemapper remapper) {
		this.getTransformer().setRemapper(remapper);
	}

	default void setSrcRemapper(AmalgRemapper remapper) {
		this.getTransformer().setSrcRemapper(remapper);
	}

	private Dependency of(Object notation) {
		if(notation instanceof Dependency d) {
			return d;
		} else {
			return this.getProject().getDependencies().create(notation);
		}
	}
}
