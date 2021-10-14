package io.github.astrarre.amalgamation.gradle.dependencies.filtr;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processors.zip.ZipBehavior;
import net.devtech.zipio.processors.zip.ZipFilter;

public class SourcesZipFilter implements ZipFilter {
	public static final SourcesZipFilter INSTANCE = new SourcesZipFilter();

	@Override
	public ZipBehavior test(OutputTag file, Supplier<FileSystem> system) {
		return file instanceof SourcesOutput ? ZipBehavior.COPY : ZipBehavior.CONTINUE;
	}
}
