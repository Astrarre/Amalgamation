package io.github.astrarre.amalgamation.gradle.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.utils.AmalgamationIO;
import joptsimple.internal.Strings;
import org.jetbrains.annotations.Nullable;

public class RedirectMergeFile extends CachedFile<String> {
	protected final Path src;
	protected final List<String> names;

	public RedirectMergeFile(Path file, Path src, List<String> names) {
		super(file, String.class);
		this.src = src;
		this.names = names;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		Hasher hasher = Hashing.sha256().newHasher();
		hasher.putLong(Files.getLastModifiedTime(src).toMillis());
		hasher.putUnencodedChars(src.toAbsolutePath().toString());
		names.forEach(hasher::putUnencodedChars);
		String newData = hasher.hash().toString();
		if(newData.equals(currentData)) return null;

		try (FileSystem system = FileSystems.newFileSystem(
				new URI("jar:" + path.toUri()),
				AmalgamationIO.CREATE_ZIP);
		     FileSystem input = FileSystems.newFileSystem(this.src, null)) {
			for (Path directory : system.getRootDirectories()) {
				for (Path rootDirectory : input.getRootDirectories()) {
					Files.copy(rootDirectory, directory);
				}
			}
			Path merger = system.getPath(AmalgamationIO.MERGER_META_FILE);
			if(Files.exists(merger)) {
				Properties properties = new Properties();
				try(BufferedReader reader = Files.newBufferedReader(merger)) {
					properties.load(reader);
				}
				String platforms = properties.getProperty(AmalgamationIO.PLATFORMS, "");
				Set<String> split = new HashSet<>(this.names);
				split.addAll(Arrays.asList(platforms.split(",")));
				properties.setProperty(AmalgamationIO.PLATFORMS, Strings.join(split, ","));
				try(BufferedWriter writer = Files.newBufferedWriter(merger)) {
					properties.store(writer, "merger metadata file for resources jar");
				}
			}
		}
		return newData;
	}
}
