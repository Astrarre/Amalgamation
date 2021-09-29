package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.DownloadUtil;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import net.devtech.zipio.ZipTag;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class SplitMcFile extends Normal.Hashed implements ZipProcessable {
	/**
	 * Override input process, normally it downloads from url but server stripping is a moment
	 */
	public ZipProcessCachedFile file;

	public SplitMcFile(Path file, Logger logger, LauncherMeta.HashedURL url) {
		super(file, logger, url, true);
	}

	public static SplitMcFile create(Project project, String version, boolean isClient) {
		LauncherMeta.Version vers = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project).getVersion(version);
		return new SplitMcFile(AmalgIO.cache(project, true), project.getLogger(), vers.getJar(isClient));
	}

	@Override
	public ZipProcess process() {
		try {
			return this.builder(null);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void write(Path output, DownloadUtil.Result result) throws IOException {
		this.builder(result.stream).execute();
	}

	@Override
	protected boolean needsUrl() {
		return this.file == null;
	}

	protected ZipProcessBuilder builder(InputStream stream) throws IOException {
		ZipProcessBuilder builder = ZipProcess.builder();
		Path dir = this.path();
		Path cls = dir.resolve("classes.jar"), rss = dir.resolve("resources.jar");
		if(this.isOutdated()) {
			ZipTag clsTag = builder.createZipTag(cls), rssTag = builder.createZipTag(rss);
			if(this.file == null) {
				builder.afterExecute(() -> {

				});
			} else {
				builder.linkProcess(this.file.createProcess(), p -> null);
				builder.setEntryProcessor(buffer -> {
					String path = buffer.path();
					if(path.endsWith(".class")) {
						buffer.copyTo(path, clsTag);
					} else {
						buffer.copyTo(path, rssTag);
					}
					return ProcessResult.HANDLED;
				});
			}
		} else {
			builder.addProcessed(cls);
			builder.addProcessed(rss);
		} return builder;
	}
}
