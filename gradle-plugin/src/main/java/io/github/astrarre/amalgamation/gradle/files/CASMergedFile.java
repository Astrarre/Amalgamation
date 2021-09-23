package io.github.astrarre.amalgamation.gradle.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

import com.google.common.base.Objects;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMergerUtil;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Server & Client Merged
 */
public class CASMergedFile extends CachedFile<String> {
	public final Project project;
	public final String version;
	public final CASMerger.Handler handler;
	public final int classReaderSettings;
	public final boolean checkForServerOnly;
	public final Supplier<File> client;
	public final Supplier<File> server;
	public CASMergedFile(Path file, Project project, String version, CASMerger.Handler handler, int settings, boolean only,
			Supplier<File> client, Supplier<File> server) {
		super(file, String.class);
		this.project = project;
		this.version = version;
		this.handler = handler;
		this.classReaderSettings = settings;
		this.checkForServerOnly = only;
		this.client = client;
		this.server = server;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		Files.createDirectories(path.getParent());
		File clientF = this.client.get(), serverF = this.server.get();
		Hasher hasher = Hashing.sha256().newHasher();
		AmalgIO.hash(hasher, clientF);
		AmalgIO.hash(hasher, serverF);
		String hash = hasher.hash().toString();
		if(Objects.equal(currentData, hash)) {
			return currentData;
		}

		Logger logger = this.project.getLogger();
		logger.lifecycle("Merging " + this.version);

		try (Clock ignored = new Clock("Merged " + this.version + " in %dms", logger);
		     ZipOutputStream merged = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
		     FileSystem client = FileSystems.newFileSystem(clientF.toPath(), (ClassLoader) null);
		     FileSystem server = FileSystems.newFileSystem(serverF.toPath(), (ClassLoader) null)) {
			CASMergerUtil.merge(this.handler, client, server, merged, this.classReaderSettings, this.checkForServerOnly);
		}

		return hash;
	}
}
