package io.github.astrarre.amalgamation.gradle.dependencies.filters;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processors.zip.ZipBehavior;
import net.devtech.zipio.processors.zip.ZipFilter;

public class ResourceZipFilter implements ZipFilter {
	public static final ResourceZipFilter FILTER = new ResourceZipFilter();
	public static final ZipFilter SKIP = invert(FILTER);

	@Override
	public ZipBehavior test(OutputTag file, Supplier<FileSystem> system) {
		if(file instanceof ResourcesOutput) {
			return ZipBehavior.COPY;
		} else {
			Path path = file.getVirtualPath();
			if(path != null) {
				Path parent = path.toAbsolutePath().getParent().resolve(path.getFileName().toString() + ".rss_marker");
				return Files.exists(parent) ? ZipBehavior.COPY : ZipBehavior.CONTINUE;
			} else {
				return ZipBehavior.CONTINUE;
			}
		}
	}

	public static ZipFilter invert(ZipFilter filter) {
		return (file, system) -> filter.test(file, system) == ZipBehavior.COPY ? ZipBehavior.COPY : ZipBehavior.SKIP;
	}

}
