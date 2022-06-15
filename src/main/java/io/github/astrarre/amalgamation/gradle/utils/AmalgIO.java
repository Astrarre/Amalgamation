package io.github.astrarre.amalgamation.gradle.utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.AmalgamationDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedDependency;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;

public class AmalgIO {
	
	public static final HashFunction SHA256 = com.google.common.hash.Hashing.sha256();
	public static final ExecutorService SERVICE = ForkJoinPool.commonPool();
	private static final OpenOption[] OPTIONS = {
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE
	};
	
	public static void hashDep(Hasher hasher, Project project, Object dependency) {
		if(dependency instanceof CachedDependency c) {
			try {
				c.hashInputs(hasher);
			} catch(IOException e) {
				throw Err.rethrow(e);
			}
		} else if(dependency instanceof AmalgamationDependency a) {
			for(Artifact artifact : a.getArtifacts()) {
				hasher.putBytes(artifact.hash);
			}
		} else {
			for(File file : AmalgIO.resolve(project, List.of((Dependency) dependency))) {
				AmalgIO.hash(hasher, file);
			}
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
	
	public static String path(Path path) {
		Path other = path.toAbsolutePath();
		return other.getRoot().relativize(other).toString().replace(path.getFileSystem().getSeparator(), "/");
	}
	
	public static Path cache(Project project, boolean global) {
		if(global) {
			return globalCache(project);
		} else {
			return AmalgDirs.ROOT_PROJECT.root(project.getRootProject());
		}
	}
	
	public static Path globalCache(Project project) {
		return AmalgDirs.GLOBAL.root(project);
	}
	
	public static List<Path> resolveSources(Project project, Iterable<Dependency> dependencies) {
		List<ComponentIdentifier> ids = project
				.getConfigurations()
				.detachedConfiguration(toArray(dependencies, Dependency[]::new))
				.getResolvedConfiguration()
				.getResolvedArtifacts()
				.stream()
				.map(ResolvedArtifact::getId)
				.map(ComponentArtifactIdentifier::getComponentIdentifier)
				.toList();
		
		return getSources(project, ids).map(ResolvedArtifactResult::getFile).map(File::toPath).map(apply(Path::toRealPath)).toList();
	}
	
	public static Stream<ResolvedArtifactResult> getSources(Project project, Iterable<ComponentIdentifier> ids) {
		return project
				.getDependencies()
				.createArtifactResolutionQuery()
				.forComponents(ids)
				.withArtifacts(JvmLibrary.class, List.of(SourcesArtifact.class))
				.execute()
				.getResolvedComponents()
				.stream()
				.map(c -> c.getArtifacts(SourcesArtifact.class))
				.flatMap(Set::stream)
				.filter(ResolvedArtifactResult.class::isInstance)
				.map(ResolvedArtifactResult.class::cast);
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
	
	public static void resolveDeps(Project project, Set<Dependency> dependencies, Set<ResolvedDependency> resolved,
	                               List<Dependency> everythingElse) {
		Configuration configuration = project.getConfigurations().detachedConfiguration(Iterables.toArray(dependencies, Dependency.class));
		Set<Dependency> unvisited = dependencies.stream().map(Dependency::copy).collect(Collectors.toCollection(HashSet::new));
		var deps = new HashSet<>(configuration.getResolvedConfiguration().getFirstLevelModuleDependencies(s -> {
			unvisited.remove(s);
			return true;
		}));
		everythingElse.addAll(unvisited);
		List<ResolvedDependency> toProcess = new ArrayList<>(deps);
		while(!toProcess.isEmpty()) {
			int orig = toProcess.size();
			for(int i = orig - 1; i >= 0; i--) {
				Set<ResolvedDependency> children = toProcess.get(i).getChildren();
				toProcess.addAll(children);
				deps.addAll(children);
			}
			toProcess.subList(0, orig).clear();
		}
		resolved.addAll(deps);
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
	
	public static File resolveFile(Project project, Object notation) {
		Dependency dependency = project.getDependencies().create(notation);
		return Iterables.getOnlyElement(resolve(project, List.of(dependency)));
	}
	
	public static String b64(byte[] data) {
		return Base64.getUrlEncoder().encodeToString(data);
	}
	
	public static void createParent(Path file) throws IOException {
		Path parent = file.getParent();
		if(!Files.exists(parent)) {
			Files.createDirectories(parent);
		}
	}
	
	public static ByteBuffer readAll(Path path) {
		try {
			SeekableByteChannel channel = Files.newByteChannel(path);
			ByteBuffer buf = ByteBuffer.allocate(Math.toIntExact(channel.size()));
			channel.position(0);
			buf.limit(channel.read(buf));
			return buf;
		} catch(IOException e) {
			throw Err.rethrow(e);
		}
	}
	
	public static void write(Path path, ByteBuffer contents) throws IOException {
		SeekableByteChannel channel = Files.newByteChannel(path, OPTIONS);
		channel.write(contents);
		channel.truncate(contents.limit());
	}
	
	public static void deleteDirectory(Path path) throws IOException {
		Files.walk(path).sorted(Comparator.reverseOrder()).forEach(UCons.of(Files::delete));
	}
	
	public static Path changeExtension(Path path, String ext) {
		String name = path.toString();
		int index = name.lastIndexOf('.');
		if(index == -1) {
			return path.getParent().resolve(name + "." + ext);
		} else {
			return path.getParent().resolve(name.substring(0, index + 1) + ext);
		}
	}
	
	/**
	 * @return an uncached zip file system
	 */
	public static FileSystem createZip(Path path) {
		if(path == null) {
			return null;
		}
		try {
			Path parent = path.getParent();
			if(parent != null) {
				Files.createDirectories(parent);
			}
			Files.deleteIfExists(path);
			return FileSystems.newFileSystem(path, Map.of("create", "true"));
		} catch(IOException e) {
			throw Err.rethrow(e);
		}
	}
	
	static <A, B> Function<A, B> apply(UnsafeFunction<A, B> function) {
		return function;
	}
	
	interface UnsafeFunction<A, B> extends Function<A, B> {
		@Override
		default B apply(A a) {
			try {
				return this.runUnsafe(a);
			} catch(Throwable e) {
				throw Err.rethrow(e);
			}
		}
		
		B runUnsafe(A a) throws Throwable;
	}
}
