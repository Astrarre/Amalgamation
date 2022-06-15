package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.cadixdev.mercury.remapper.MercuryRemapper;

public class RemapAllSourcesJars extends AbstractRemapAllTask<RemapSourcesJar> {
	@Override
	public void remapAll() throws IOException {
		Mercury mercury = new Mercury();
		MappingSet set = MappingSet.create();
		for(Mappings.Namespaced namespaced : this.readAllMappings()) {
			Mappings.loadMappings(set, namespaced);
		}
		var remapper = MercuryRemapper.create(set);
		mercury.getProcessors().add(remapper);

		record MercuryPair(Path rewrite, Path output) {}
		List<MercuryPair> pairs = new ArrayList<>();
		Path temp = this.getProject().getBuildDir().toPath().resolve("amalgremapallsourcestemp");
		RemapSourcesJar.clearDirectory(temp);
		int index = 0;
		for(RemapSourcesJar jar : this.remapJars) {
			this.ensureCleanInput(jar);
			List<Path> classpath = mercury.getClassPath();
			List<Path> sources = mercury.getSourcePath();
			for(File file : jar.getClasspath().get()) {
				classpath.add(file.toPath());
			}

			if(jar.isOutdated) {
				// add outputs (since RemapSourcesJar is in-place)
				Path directory = temp.resolve("jar" + index++);
				File file = jar.getOutputs().getFiles().getSingleFile();
				Path zip = file.toPath();
				RemapSourcesJar.dumpZip(zip, directory);
				pairs.add(new MercuryPair(
						directory,
						zip
				));
				sources.add(directory);
			} else {
				// add inputs to classpath
				for(File file : jar.getInputs().getFiles()) {
					classpath.add(file.toPath());
				}
			}
		}

		for(MercuryPair pair : pairs) {
			try(FileSystem system = FileSystems.newFileSystem(pair.output, Map.of("create", "true"))) {
				mercury.rewrite(pair.rewrite, system.getPath("/"));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		RemapSourcesJar.clearDirectory(temp);
	}
}
