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
import io.github.astrarre.amalgamation.gradle.utils.mojmerge.MojMergerUtil;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MojMergedFile extends CachedFile<String> {
	public final Project project;
	public final String version;
	public final CASMerger.Handler handler;
	public final Supplier<File> clientJar;
	public final Supplier<Path> serverMappings;

	public MojMergedFile(Path file,
			Project project,
			String version,
			CASMerger.Handler handler,
			Supplier<File> clientJar,
			Supplier<Path> serverMappings) {
		super(file, String.class);
		this.project = project;
		this.version = version;
		this.handler = handler;
		this.clientJar = clientJar;
		this.serverMappings = serverMappings;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		Files.createDirectories(path.getParent());
		File clientF = this.clientJar.get(), serverMappings = this.serverMappings.get().toFile();
		String hash = getHash(clientF, serverMappings);
		if(Objects.equal(currentData, hash)) {
			return currentData;
		}

		Logger logger = this.project.getLogger();
		logger.lifecycle("Moj Merging " + this.version);

		try (Clock ignored = new Clock("Moj Merged " + this.version + " in %dms", logger)) {
			MojMergerUtil merger = new MojMergerUtil(clientF.toPath(), serverMappings.toPath(), path, this.handler);
			merger.merge();
		}

		return hash;
	}

	@NotNull
	public static String getHash(File clientF, File serverMappings) {
		Hasher hasher = Hashing.sha256().newHasher();
		AmalgIO.hash(hasher, clientF);
		AmalgIO.hash(hasher, serverMappings);
		return hasher.hash().toString();
	}
}
