package io.github.astrarre.amalgamation.gradle.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.dependencies.filtr.SourcesOutput;
import net.devtech.zipio.OutputTag;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.invocation.Gradle;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;

public class AmalgIO {
	public static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[8192]);
	/**
	 * if the first entry of a zip file is a file with the name of this field, it is configured
	 */
	public static final String MERGER_META_FILE = "merger_metadata.properties";
	// start merger meta properties
	public static final String TYPE = "type"; // resources, java, classes, all

	public static String hash(Iterable<File> files) {
		Hasher hasher = Hashing.sha256().newHasher();
		hash(hasher, files);
		return hasher.hash().toString();
	}

	public static void hash(Hasher hasher, Iterable<File> files) {
		for(File file : files) {
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

	public static String hash(Hasher hasher) {
		byte[] data = hasher.hash().asBytes();
		return Base64.getUrlEncoder().encodeToString(data);
	}

	public static Path cache(Project project, boolean global) {
		if(global) {
			return globalCache(project);
		} else {
			return projectCache(project.getRootProject());
		}
	}

	public static Path globalCache(Project project) {
		return globalCache(project.getGradle());
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
		while((read = from.read(buf)) != -1) {
			to.write(buf, 0, read);
		}
	}

	public static File resolve(Project project, Dependency dependencies) {
		return Iterables.getOnlyElement(resolve(project, List.of(dependencies)));
	}

	public static List<Path> resolveSources(Project project, Iterable<Dependency> dependencies) {
		List<ComponentIdentifier> ids = project.getConfigurations()
				.detachedConfiguration(toArray(dependencies, Dependency[]::new))
				.getResolvedConfiguration()
				.getResolvedArtifacts()
				.stream()
				.map(ResolvedArtifact::getId)
				.map(ComponentArtifactIdentifier::getComponentIdentifier)
				.toList();

		return project.getDependencies()
				.createArtifactResolutionQuery()
				.forComponents(ids)
				.withArtifacts(JvmLibrary.class, List.of(SourcesArtifact.class))
				.execute()
				.getResolvedComponents()
				.stream()
				.map(c -> c.getArtifacts(SourcesArtifact.class))
				.flatMap(Set::stream)
				.filter(ResolvedArtifactResult.class::isInstance)
				.map(ResolvedArtifactResult.class::cast)
				.map(ResolvedArtifactResult::getFile)
				.map(File::toPath)
				.toList();
	}

	public static <T> T[] toArray(Iterable<T> iterable, IntFunction<T[]> creator) {
		if(iterable instanceof Collection<T> c) {
			return c.toArray(creator);
		} else {
			T[] buf = creator.apply(0);
			for(T t : iterable) {
				int len = buf.length;
				buf = Arrays.copyOf(buf, len + 1);
				buf[len] = t;
			}
			return buf;
		}
	}

	public static List<File> resolve(Project project, Iterable<Dependency> dependencies) {
		Configuration configuration = null;
		List<File> resolved = new ArrayList<>();
		for(Dependency dependency : dependencies) {
			if(dependency instanceof SelfResolvingDependency s) {
				resolved.addAll(s.resolve());
			} else {
				if(configuration == null) {
					configuration = project.getConfigurations().detachedConfiguration(dependency);
				}
				configuration.getDependencies().add(dependency);
			}
		}

		if(configuration != null) {
			resolved.addAll(configuration.getResolvedConfiguration().getFiles());
		}
		return resolved;
	}

	public static File resolve(Project project, Object notation) {
		Dependency dependency = project.getDependencies().create(notation);
		return resolve(project, dependency);
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
