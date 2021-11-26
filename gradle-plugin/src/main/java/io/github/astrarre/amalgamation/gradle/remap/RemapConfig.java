package io.github.astrarre.amalgamation.gradle.remap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.MappingTarget;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.AbstractBinRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.cls.TRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.src.TrieHarderRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

public class RemapConfig {
	protected final Project project;
	final List<MappingTarget> mappings = new ArrayList<>();
	final List<Dependency> classpath = new ArrayList<>();

	AmalgRemapper remapper = new TRemapper(), srcRemapper;

	public RemapConfig(Project project) {this.project = project;}

	/**
	 * @param object the dependency
	 * @param from the origin namespace
	 * @param to the destination namespace
	 */
	public MappingTarget mappings(Object object, String from, String to) {
		MappingTarget mappings = new MappingTarget(this.project, this.of(object), from, to);
		this.mappings.add(mappings);
		return mappings;
	}

	public void classpath(Object object) {
		this.classpath.add(this.project.getDependencies().create(object));
	}

	public void classpath(Object object, Closure<ModuleDependency> config) {
		this.classpath.add(this.project.getDependencies().create(object, config));
	}

	public void mappings(MappingTarget dependency) {
		this.mappings.add(dependency);
	}

	public AmalgRemapper getRemapper() {
		return this.remapper;
	}

	public AmalgRemapper getSrcRemapper() {
		return this.srcRemapper;
	}

	public void setRemapper(AmalgRemapper remapper) {
		this.remapper = remapper;
		if(this.srcRemapper != null) {
			this.setSrcRemapper(remapper);
		}
	}

	public void setSrcRemapper(AmalgRemapper remapper) {
		if(this.remapper instanceof AbstractBinRemapper a) {
			a.setSourceRemapper(remapper);
			this.srcRemapper = remapper;
		} else {
			throw new UnsupportedOperationException("cannot set source remapper on non-AbstractRemapper remapper!");
		}
	}

	/**
	 * sets the source remapper to the TrieHarder source remapper
	 */
	public void trieHarder() {
		this.setSrcRemapper(new TrieHarderRemapper());
	}

	/**
	 * sets the class remapper to fabricmc/tiny-remapper
	 */
	public void tinyRemapper() {
		this.setRemapper(new TRemapper());
	}

	public Dependency of(Object notation) {
		if(notation instanceof Dependency d) {
			return d;
		} else {
			return this.project.getDependencies().create(notation);
		}
	}

	public void hashMappings(Hasher hasher) throws IOException {
		for(var mapping : this.mappings) {
			mapping.hash(hasher);
		}
	}

	public void hash(Hasher hasher) throws IOException {
		this.hashMappings(hasher);
		for(Dependency dependency : this.classpath) {
			this.hashDep(hasher, dependency);
		}
	}

	public Dependency hashDep(Hasher hasher, Object dependency) throws IOException {
		Dependency resolved = this.of(dependency);
		AmalgIO.hashDep(hasher, this.project, resolved);
		return resolved;
	}

	public List<MappingTarget> getMappings() {
		return Collections.unmodifiableList(this.mappings);
	}

	public List<Dependency> getClasspath() {
		return Collections.unmodifiableList(this.classpath);
	}
}
