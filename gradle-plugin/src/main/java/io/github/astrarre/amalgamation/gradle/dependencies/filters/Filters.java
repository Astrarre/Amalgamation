package io.github.astrarre.amalgamation.gradle.dependencies.filters;

import java.nio.file.Path;

import net.devtech.zipio.OutputTag;

public class Filters {
	public static OutputTag from(OutputTag tag, Path dest) {
		if(tag instanceof SourcesOutput) {
			return new SourcesOutput(dest);
		} else {
			return new OutputTag(dest);
		}
	}
}
