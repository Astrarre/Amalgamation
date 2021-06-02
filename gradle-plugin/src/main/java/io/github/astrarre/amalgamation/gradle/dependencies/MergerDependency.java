package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.MergeFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgamationIO;
import io.github.astrarre.amalgamation.gradle.utils.CollectionUtil;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MergerDependency extends AbstractSelfResolvingDependency {
	public final CachedFile<?> merger;
	public final Map<List<TypeEntry>, Collection<Dependency>> unique = new HashMap<>(), merge = new HashMap<>();
	public final List<TypeEntry> additionalEntries = new ArrayList<>();
	public final Map<List<TypeEntry>, Iterable<File>> uniqueResolved = new HashMap<>(), mergeResolved = new HashMap<>();
	public boolean compareInstructions = true, leaveMarker = true, globalCache = false;

	public MergerDependency(Project project) {
		super(project, "io.github.f2bb", null, "0.0.0");
		this.merger = new MergeFile(this, project);
	}

	public static String toString(Dependency dependency) {
		return dependency.getGroup() + ':' + dependency.getName() + ':' + dependency.getVersion() + ':' + dependency.getReason() + ':' + dependency.getClass();
	}

	@Override
	public String getName() {
		Hasher hasher = Hashing.sha256().newHasher();
		this.mergeResolved.forEach((strings, dependencies) -> {
			for (TypeEntry string : strings) {
				hasher.putUnencodedChars(string.type);
				hasher.putUnencodedChars(string.entry);
			}
			AmalgamationIO.hash(hasher, dependencies);
		});
		this.uniqueResolved.forEach((strings, dependencies) -> {
			for (TypeEntry string : strings) {
				hasher.putUnencodedChars(string.type);
				hasher.putUnencodedChars(string.entry);
			}
			AmalgamationIO.hash(hasher, dependencies);
		});

		hasher.putBoolean(this.compareInstructions);
		return hasher.hash().toString();
	}

	@Override
	protected Set<File> path() {
		if (this.resolved == null) {
			List<File> resources = new ArrayList<>();
			this.unique.forEach((strings, dependencies) -> this.uniqueResolved.put(strings, CollectionUtil.filt(this.resolve(dependencies), resources, AmalgamationIO::isResourcesJar)));
			this.merge.forEach((strings, dependencies) -> this.mergeResolved.put(strings, CollectionUtil.filt(this.resolve(dependencies), resources, AmalgamationIO::isResourcesJar)));
			Path path = this.merger.getPath(); // order matters
			Set<File> files = new HashSet<>();
			for (File resource : resources) {

			}
			try {
				Files.walk(path).filter(Files::isRegularFile).forEach(path1 -> files.add(path1.toFile()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.resolved = files;
			return files;
		}
		return this.resolved;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		throw new UnsupportedOperationException();
	}

	public void include(Object object, TypeEntry... platforms) {
		this.merge.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	public void addUnique(Object object, TypeEntry... platforms) {
		this.unique.computeIfAbsent(Arrays.asList(platforms), s -> new ArrayList<>()).add(this.project.getDependencies().create(object));
	}

	@Override
	public Dependency copy() {
		return new MergerDependency(this.project);
	}

	public void appendEntry(String type, String entry) {
		this.additionalEntries.add(this.of(type, entry));
	}

	public TypeEntry of(String type, String entry) {
		return new TypeEntry(type, entry);
	}

	public TypeEntry ofRand(String entry) {
		return new TypeEntry(UUID.randomUUID().toString(), entry);
	}

	/**
	 * a 'type' and it's value, aids in merging. For example, the entry for '1.16.5' would be 'version', '1.16.5'
	 */
	public static final class TypeEntry {
		public final String type, entry;

		public TypeEntry(String type, String entry) {
			this.type = type;
			this.entry = entry;
		}

		@Override
		public String toString() {
			return this.type + " of " + this.entry;
		}
	}

}
