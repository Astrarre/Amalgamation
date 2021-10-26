package io.github.astrarre.amalgamation.gradle.dependencies.filters;

import java.nio.file.Path;

import net.devtech.zipio.OutputTag;

public class ResourcesOutput extends OutputTag {
	public ResourcesOutput(Path path) {
		super(path);
	}
}
