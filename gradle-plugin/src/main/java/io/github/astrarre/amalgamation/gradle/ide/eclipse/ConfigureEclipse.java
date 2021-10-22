package io.github.astrarre.amalgamation.gradle.ide.eclipse;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;

import io.github.astrarre.amalgamation.gradle.dependencies.AbstractSelfResolvingDependency;

public class ConfigureEclipse {
	public static EclipseExtension extension;
	public static void configure(Project project) {
		var eclipse = project.getExtensions().getByType(EclipseModel.class);

		eclipse.getClasspath().getFile().whenMerged(c -> {
			var classpath = (Classpath) c;

			Set<Path> files = eclipse.getClasspath().getPlusConfigurations().stream()
					.map(Configuration::getAllDependencies)
					.flatMap(Collection::stream)
					.filter(d -> d instanceof AbstractSelfResolvingDependency)
					.flatMap(d -> ((AbstractSelfResolvingDependency) d).resolve().stream())
					.map(File::toPath)
					.collect(Collectors.toSet());

			Map<Path, Path> srcMap = new HashMap<>();
			Set<Path> sourcePaths = new HashSet<>();
			Map<Path, Path> truncatedToFullDep = new HashMap<>();
			Map<Path, Path> truncatedToSources = new HashMap<>();

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
					System.out.println(libPath);
					Path srcPath = srcMap.get(libPath);
					System.out.println(srcPath);
					System.out.println(lib.getSourcePath());
					if(lib.getSourcePath() == null && srcPath != null) {
						System.out.println("set");
						lib.setSourcePath(classpath.fileReference(srcPath.toFile()));
					} else if(sourcePaths.contains(libPath)) {
						System.out.println("rem");
						iter.remove();
					}
				}
			}
		});

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
}
