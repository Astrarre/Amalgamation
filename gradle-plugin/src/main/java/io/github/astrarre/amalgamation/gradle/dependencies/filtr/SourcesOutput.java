package io.github.astrarre.amalgamation.gradle.dependencies.filtr;

import java.nio.file.Path;

import net.devtech.zipio.OutputTag;

public class SourcesOutput extends OutputTag {
	public SourcesOutput(Path path) {
		super(path);
	}
}