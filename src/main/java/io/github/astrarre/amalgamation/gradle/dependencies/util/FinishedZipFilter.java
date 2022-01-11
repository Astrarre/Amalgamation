package io.github.astrarre.amalgamation.gradle.dependencies.util;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processors.zip.ZipBehavior;
import net.devtech.zipio.processors.zip.ZipFilter;

public record FinishedZipFilter(UnaryOperator<Artifact> artifacts) implements ZipFilter {
	public static ZipFilter createDefault(UnaryOperator<Artifact> artifacts) {
		return ResourceZipFilter.FILTER.andThen(new FinishedZipFilter(artifacts));
	}

	@Override
	public ZipBehavior test(OutputTag file, Supplier<FileSystem> system) {
		return (file instanceof Artifact a && Files.exists(artifacts.apply(a).path)) ? ZipBehavior.USE_OUTPUT : ZipBehavior.CONTINUE;
	}
}
