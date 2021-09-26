package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.gson.JsonObject;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.Nullable;

public class DeJiJDependency extends AbstractSelfResolvingDependency {
	public final List<Dependency> dependencies;

	public DeJiJDependency(Project project, String name) {
		this(project, name, new ArrayList<>());
	}

	public DeJiJDependency(Project project, String name, List<Dependency> dependencies) {
		super(project, "io.github.astrarre.amalgamation", name, "0.0.0");
		this.dependencies = dependencies;
	}

	public void add(Object dependency) {
		this.dependencies.add(this.project.getDependencies().create(dependency));
	}

	public void add(Object dependency, Closure<ModuleDependency> config) {
		this.dependencies.add(this.project.getDependencies().create(dependency, config));
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		List<File> toProcess = new ArrayList<>();
		Set<File> doNotRemove = new HashSet<>();
		for (File file : this.resolve(this.dependencies)) {
			toProcess.add(file);
			doNotRemove.add(file);
		}

		while (!toProcess.isEmpty()) {
			for (int i = toProcess.size() - 1; i >= 0; i--) {
				File process = toProcess.remove(i);
				Path cache = AmalgIO.projectCache(this.project).resolve("de-jij").resolve(this.name).resolve(AmalgIO.hash(Collections.singleton(process)));
				DeJiJCachedFile cachedFile = new DeJiJCachedFile(cache, process);
				for(Path path : UnsafeIterable.walkFiles(cachedFile.getPath())) {
					if(!path.endsWith("original.jar")) {
						toProcess.add(path.toFile());
					}
				}

				if(!doNotRemove.contains(process)) {
					process.delete();
				}
			}
		}
		return UnsafeIterable.walkFiles(AmalgIO.projectCache(this.project).resolve("de-jij").resolve(this.name));
	}

	@Override
	public Dependency copy() {
		return new DeJiJDependency(this.project, this.name, new ArrayList<>(this.dependencies));
	}

	public static class DeJiJCachedFile extends CachedFile<String> {
		public final File toDeJiJ;
		public DeJiJCachedFile(Path file, File toDeJiJ) {
			super(file, String.class);
			this.toDeJiJ = toDeJiJ;
		}

		@Override
		protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
			Files.createDirectories(path);
			try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(this.toDeJiJ))); ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path.resolve("original.jar"))))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String name = entry.getName();
					if(name.endsWith("fabric.mod.json")) {
						JsonObject object = GSON.fromJson(new InputStreamReader(zis), JsonObject.class);
						if(object.has("jars")) {
							object.remove("jars");
							zos.putNextEntry(new ZipEntry(entry.getName()));
							GSON.toJson(object, new OutputStreamWriter(zos));
							zos.closeEntry();
							break;
						}
					}
					if(name.endsWith(".jar")) {
						Path toWrite = path.resolve(name);
						Files.createDirectories(toWrite.getParent());
						Files.copy(zis, toWrite);
					} else {
						zos.putNextEntry(new ZipEntry(entry.getName()));
						AmalgIO.copy(zis, zos);
						zos.closeEntry();
					}
				}
			}
			return null;
		}

	}
}
