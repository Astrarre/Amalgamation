package io.github.f2bb.amalgamation.gradle.config;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class RemappingConfiguration {
	private final List<Dependency> mappings = new ArrayList<>(), inputs = new ArrayList<>(), classpath = new ArrayList<>();
	private final Project project;

	public RemappingConfiguration(Project project) {this.project = project;}

	public void mappings(Object object) {
		this.mappings.add(this.project.getDependencies().create(object));
	}

	public void remap(Object object) {
		this.inputs.add(this.project.getDependencies().create(object));
	}

	public void remapAll(Iterable<Dependency> dependencies) {
		dependencies.forEach(this::remap);
	}

	public void classpath(Object object) {
		this.inputs.add(this.project.getDependencies().create(object));
	}
}
