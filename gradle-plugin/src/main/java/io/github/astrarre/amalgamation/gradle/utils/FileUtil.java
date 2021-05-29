package io.github.astrarre.amalgamation.gradle.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;

public class FileUtil {
	public static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[8192]);

	public static boolean isResourcesJar(File file) {
		if(file.isDirectory()) return false;
		try(FileSystem system = FileSystems.newFileSystem(file.toPath(), null)) {
			if(Files.exists(system.getPath(MergeUtil.RESOURCES_MARKER_FILE))) {
				return true;
			} else {
				Path path = system.getPath(MergeUtil.MERGER_META_FILE);
				if(Files.exists(path)) {
					Properties properties = new Properties();
					properties.load(Files.newInputStream(path));
					return properties.getProperty("resources", "false").equals("true");
				} else {
					return false;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String hash(Iterable<File> files) {
		Hasher hasher = Hashing.sha256().newHasher();
		hash(hasher, files);
		return hasher.hash().toString();
	}

	public static void hash(Hasher hasher, Iterable<File> files) {
		for (File file : files) {
			hasher.putUnencodedChars(file.getAbsolutePath());
			hasher.putLong(file.lastModified());
		}
	}

	public static Path cache(Project project, boolean global) {
		if(global) return globalCache(project.getGradle());
		else return projectCache(project.getRootProject());
	}

	public static Path globalCache(Gradle gradle) {
		return gradle.getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation");
	}

	public static Path projectCache(Project project) {
		return project.getBuildDir().toPath().resolve("amalgamation-caches");
	}

	public static void copy(InputStream from, OutputStream to) throws IOException {
		int read;
		byte[] buf = BUFFER.get();
		while ((read = from.read(buf)) != -1) {
			to.write(buf, 0, read);
		}
	}

	public static byte[] readAll(InputStream zis) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(zis, baos);
		return baos.toByteArray();
	}
}