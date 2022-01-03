package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.AmalgamationDependency;

import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.binary.TinyRemapperImpl;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.misc.AccessWidenerRemapperImpl;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.misc.MetaInfFixerImpl;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.source.TrieHarderRemapperImpl;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.unpick.UnpickExtension;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

import net.fabricmc.tinyremapper.TinyRemapper;

public class RemapConfig {
	protected final Project project;
	final List<MappingTarget> mappings = new ArrayList<>();
	final List<Object> classpath = new ArrayList<>();
	byte[] mappingsHash;

	final List<AmalgRemapper> remappers = new ArrayList<>(List.of(
			new TinyRemapperImpl(t -> {}),
			new TrieHarderRemapperImpl(),
			new AccessWidenerRemapperImpl(),
			new MetaInfFixerImpl()));
	final AmalgRemapper combined = new AmalgRemapper.Combined(this.remappers);

	public RemapConfig(Project project) {this.project = project;}

	/**
	 * @param object the dependency
	 * @param from the origin namespace
	 * @param to the destination namespace
	 */
	public MappingTarget mappings(Object object, String from, String to) {
		MappingTarget mappings = new MappingTarget(this.project, (Dependency) this.of(object), from, to);
		this.mappings(mappings);
		return mappings;
	}

	public void classpath(Object object) {
		this.classpath.add(this.of(object));
	}

	public void classpath(Object object, Closure<ModuleDependency> config) {
		this.classpath.add(this.of(object, config));
	}

	public void mappings(MappingTarget dependency) {
		if(this.mappingsHash != null) {
			throw new IllegalStateException("RemapDependency was already evaluated, cannot add mappings!");
		}
		this.mappings.add(dependency);
	}


	/**
	 * sets the source remapper to the TrieHarder source remapper
	 */
	public void trieHarder() {
		avoidSecond(TrieHarderRemapperImpl.class);
		this.remappers.add(new TrieHarderRemapperImpl());
	}

	/**
	 * sets the class remapper to fabricmc/tiny-remapper
	 */
	public void tinyRemapper() {
		avoidSecond(TinyRemapperImpl.class);
		this.remappers.add(new TinyRemapperImpl(null));
	}

	public void tinyRemapperWithUnpick() {
		avoidSecond(TinyRemapper.class);
		this.remappers.add(new TinyRemapperImpl(t -> t.extension(new UnpickExtension(this.mappings))));
	}

	public void metaInfFixer() {
		avoidSecond(MetaInfFixerImpl.class);
		this.remappers.add(new MetaInfFixerImpl());
	}

	public void removeRemapper(int index) {
		this.remappers.remove(index);
	}

	private void avoidSecond(Class<?> type) {
		if(this.remappers.stream().anyMatch(type::isInstance)) {
			throw new IllegalStateException("multiple instances of " + type + " in remapper configuration");
		}
	}

	public void hashMappings(Hasher hasher) {
		for(var mapping : this.mappings) {
			mapping.hash(hasher);
		}
	}

	public void hash(Hasher hasher) throws IOException {
		this.hashMappings(hasher);
		for(Object dependency : this.classpath) {
			AmalgIO.hashDep(hasher, this.project, dependency);
		}
		this.combined.hash(hasher);
	}

	public byte[] getMappingsHash() {
		byte[] hash = this.mappingsHash;
		if(hash == null) {
			Hasher hasher = AmalgIO.SHA256.newHasher();
			hashMappings(hasher);
			this.mappingsHash = hash = hasher.hash().asBytes();
		}
		return hash;
	}

	public List<MappingTarget> getMappings() {
		return Collections.unmodifiableList(this.mappings);
	}

	public List<Object> getClasspath() {
		return Collections.unmodifiableList(this.classpath);
	}

	public AmalgRemapper getRemapper() {
		return combined;
	}

	protected Object of(Object notation) {
		if(notation instanceof Dependency d) {
			return d;
		} else if(notation instanceof AmalgamationDependency l) {
			return l;
		} else {
			return this.project.getDependencies().create(notation);
		}
	}

	protected Object of(Object notation, Closure<ModuleDependency> config) {
		if(notation instanceof Dependency d) {
			return d;
		} else if(notation instanceof AmalgamationDependency l) {
			return l;
		} else {
			return this.project.getDependencies().create(notation, config);
		}
	}
}
