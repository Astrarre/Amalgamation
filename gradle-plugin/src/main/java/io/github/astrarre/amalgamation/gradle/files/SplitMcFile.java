package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import net.devtech.zipio.ZipTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;

public class SplitMcFile extends ZipProcessCachedFile {
	public final CachedFile file;
	public SplitMcFile(Path output, Project project, CachedFile file) {
		super(output, project);
		this.file = file;
	}

	@Override
	public void hashInputs(Hasher hasher) {
		this.file.hashInputs(hasher);
	}

	@Override
	public void init(ZipProcessBuilder builder, Path outputFile) throws IOException {
		Path dir = this.path();
		Path cls = dir.resolve("classes.jar"), rss = dir.resolve("resources.jar");
		if(this.isOutdated()) {
			ZipTag clsTag = builder.createZipTag(cls), rssTag = builder.createZipTag(rss);
			CachedFile.add(this.file, builder, p -> null);
			builder.setEntryProcessor(buffer -> {
				String path = buffer.path();
				if(path.endsWith(".class")) {
					buffer.copyTo(path, clsTag);
				} else {
					buffer.copyTo(path, rssTag);
				}
				return ProcessResult.HANDLED;
			});
		} else {
			builder.addProcessed(cls);
			builder.addProcessed(rss);
		}
	}
}
