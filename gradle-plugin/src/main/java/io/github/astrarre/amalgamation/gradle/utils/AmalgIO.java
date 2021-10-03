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
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.invocation.Gradle;

public class AmalgIO {
	public static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[8192]);
	/**
	 * if the first entry of a zip file is a file with the name of this field, it is configured
	 */
	public static final String MERGER_META_FILE = "merger_metadata.properties";
	// start merger meta properties
	public static final String TYPE = "type"; // resources, java, classes, all

	public static boolean isResourcesJar(File file) {
		if (file.isDirectory()) {
			return false;
		}
		try (FileSystem system = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null)) {
			Path path = system.getPath(MERGER_META_FILE);
			if (Files.exists(path)) {
				Properties properties = new Properties();
				properties.load(Files.newInputStream(path));
				return properties.getProperty(TYPE, "all").equals("resources");
			} else {
				return false;
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
			hash(hasher, file);
		}
	}

	public static void hash(Hasher hasher, File file) {
		if(!file.exists()) {
			hasher.putLong(System.currentTimeMillis());
		} else {
			hasher.putUnencodedChars(file.getAbsolutePath());
			hasher.putLong(file.lastModified());
		}
	}

	public static void hash(Project project, Hasher hasher, Dependency dependency) {
		// todo add ZipProcessable
		hash(hasher, resolve(project, Collections.singletonList(dependency)));
	}

	public static String hash(Hasher hasher) {
		byte[] data = hasher.hash().asBytes();
		return Base64.getUrlEncoder().encodeToString(data);
	}

	public static Path cache(Project project, boolean global) {
		if (global) {
			return globalCache(project.getGradle());
		} else {
			return projectCache(project.getRootProject());
		}
	}

	public static Path globalCache(Gradle gradle) {
		return gradle.getGradleUserHomeDir().toPath().resolve("caches").resolve("amalgamation");
	}

	public static Path projectCache(Project project) {
		return project.getBuildDir().toPath().resolve("amalgamation-caches");
	}

	public static byte[] readAll(InputStream zis) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(zis, baos);
		return baos.toByteArray();
	}

	public static void copy(InputStream from, OutputStream to) throws IOException {
		int read;
		byte[] buf = BUFFER.get();
		while ((read = from.read(buf)) != -1) {
			to.write(buf, 0, read);
		}
	}

	public static File resolve(Project project, Dependency dependencies) {
		return Iterables.getOnlyElement(resolve(project, Collections.singleton(dependencies)));
	}

	public static Iterable<File> resolve(Project project, Iterable<Dependency> dependencies) {
		Configuration configuration = null;
		Iterable<File> selfResolving = null;
		for (Dependency dependency : dependencies) {
			if (dependency instanceof SelfResolvingDependency) {
				if (selfResolving == null) {
					selfResolving = ((SelfResolvingDependency) dependency).resolve();
				} else {
					selfResolving = Iterables.concat(((SelfResolvingDependency) dependency).resolve(), selfResolving);
				}
			} else {
				if (configuration == null) {
					configuration = project.getConfigurations().detachedConfiguration(dependency);
				} else {
					configuration.getDependencies().add(dependency);
				}
			}
		}

		if (configuration == null) {
			if (selfResolving == null) {
				return Collections.emptyList();
			} else {
				return selfResolving;
			}
		} else {
			if (selfResolving == null) {
				return configuration;
			} else {
				return Iterables.concat(configuration.getResolvedConfiguration().getFiles(), selfResolving);
			}
		}
	}

	public static File resolve(Project project, Object notation) {
		Dependency dependency = project.getDependencies().create(notation);
		return resolve(project, dependency);
	}

	public static boolean jarContainsClasses(File t) {
		return !isResourcesJar(t);
	}

	public static String b64(byte[] data) {
		return Base64.getUrlEncoder().encodeToString(data);
	}

	public static String insertName(Path path, String hash) {
		String name = path.getFileName().toString();
		int i = name.lastIndexOf('.');
		if(i == -1) {
			return name + "_" + hash;
		} else {
			return name.substring(0, i) + hash + name.substring(i);
		}
	}

	public static void createFile(Path resolve) throws IOException {
		if(!Files.exists(resolve)) {
			Files.createFile(resolve);
		}
	}
}
