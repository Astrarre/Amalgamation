package io.github.astrarre.amalgamation.gradle.ide.eclipse;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;

import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamation;

public class ConfigureEclipse {
	public static EclipseExtension extension;
	public static void configure(Project project) {
		var eclipse = project.getExtensions().getByType(EclipseModel.class);
		var ag = (BaseAmalgamation) project.getExtensions().getByName("ag");

/*		eclipse.getClasspath().getFile().whenMerged(c -> {
			var classpath = (Classpath) c;

			Set<Path> files = eclipse.getClasspath().getPlusConfigurations().stream()
					.map(Configuration::getAllDependencies)
					.flatMap(Collection::stream)
					.filter(d -> d instanceof AbstractSelfResolvingDependency)
					.flatMap(d -> ((AbstractSelfResolvingDependency) d).resolve().stream())
					.map(File::toPath)
					.collect(Collectors.toSet());

			Map<Path, Path> srcMap = new HashMap<>();

			eclipse.getClasspath().getPlusConfigurations().stream()
					.map(Configuration::getAllDependencies)
					.flatMap(Collection::stream)
					.filter(d -> d instanceof LibrariesDependency)
					.map(d -> (LibrariesDependency) d)
					.forEach(l -> {
						Path libsDir = Paths.get(l.librariesDirectory);

						Set<File> libs = l.resolve();
						for(var f : libs) {
							Path libDir = f.toPath().getParent();
							String version = libDir.getFileName().toString();
							String module = libDir.getParent().getFileName().toString();
							Path modulePath = libsDir.relativize(libDir.getParent().getParent());
							String group = "";
							while(modulePath != null) {
								String dot = modulePath.getParent() != null ? "." : "";
								group = dot + modulePath.getFileName().toString() + group;
								modulePath = modulePath.getParent();
							}
							String depString = String.format("%s:%s:%s", group, module, version);

							String fname = f.toPath().getFileName().toString();
							String expectedSrcName = fname.substring(0, fname.length() - 4) + "-sources.jar";
							for(File src : ag.resolve(Collections.singleton(ag.sources(depString)))) {
								Path srcP = src.toPath();
								if(srcP.getFileName().toString().equals(expectedSrcName)) {
									srcMap.put(f.toPath(), srcP);
								}
							}
						}
					});

			Map<Path, Path> truncatedToFullDep = new HashMap<>();
			Map<Path, Path> truncatedToSources = new HashMap<>();
			Set<Path> sourcePaths = new HashSet<>();

			for(Path p : files) {
				p = p.toAbsolutePath();
				String name = truncateFileName(p.getFileName().toString());
				if(!name.endsWith("-sources")) {
					truncatedToFullDep.put(p.resolveSibling(name), p);
				} else {
					name = name.substring(0, name.length() - 8); // chop off -sources
					truncatedToSources.put(p.resolveSibling(name), p);
					sourcePaths.add(p);
				}
			}

			for(var entry : truncatedToFullDep.entrySet()) {
				Path src = truncatedToSources.get(entry.getKey());
				if(src != null) {
					srcMap.put(entry.getValue(), src);
				}
			}

			Iterator<ClasspathEntry> iter = classpath.getEntries().iterator();
			while(iter.hasNext()) {
				var entry = iter.next();
				if(entry instanceof Library lib) {
					Path libPath = Paths.get(lib.getPath()).toAbsolutePath();
					Path srcPath = srcMap.get(libPath);
					if(lib.getSourcePath() == null && srcPath != null) {
						lib.setSourcePath(classpath.fileReference(srcPath.toFile()));
					} else if(sourcePaths.contains(libPath)) {
						iter.remove();
					}
				}
			}
		});*/

		extension = new EclipseExtension();
	}

	private static String truncateFileName(String name) {
		int idx = name.lastIndexOf('_');
		if(idx != -1) {
			return name.substring(0, idx);
		} else {
			return name.substring(0, name.length() - 4); // cut off .jar
		}
	}
	
	private static String truncateFileNameAndSources(String name) {
		String trunc = truncateFileName(name);
		if(trunc.endsWith("-sources")) {
			return trunc.substring(0, trunc.length() - 8);
		} else {
			return trunc;
		}
	}
}
