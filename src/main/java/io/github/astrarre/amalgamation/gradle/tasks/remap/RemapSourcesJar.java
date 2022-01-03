package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import net.devtech.zipio.impl.util.U;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.gradle.api.tasks.Internal;
import org.gradle.jvm.tasks.Jar;

public abstract class RemapSourcesJar extends AbstractRemapJarTask<RemapAllSourcesJars> implements RemapTask {
	@Override
	public void remap() throws IOException {
		MappingSet set = MappingSet.create();
		for(Mappings.Namespaced namespaced : this.readMappings()) {
			Mappings.loadMappings(set, namespaced);
		}

		Mercury mercury = RemapTask.createMercury(set, this.getClasspath());
		Path current = this.getCurrent();
		Path temp = this.getProject().getBuildDir().toPath().resolve("amalg-remap-sources-temp");
		try {
			clearDirectory(temp);
			dumpZip(current, temp);
			try(FileSystem system = U.createZip(current)) {
				mercury.rewrite(temp, system.getPath("/"));
			} catch(Exception e) {
				e.printStackTrace();
			}
		} finally {
			clearDirectory(temp);
		}
	}

	public static void dumpZip(Path zip, Path dir) {
		try(FileSystem system = U.openZip(zip)) {
			for(Path directory : system.getRootDirectories()) {
				for(Path path : UnsafeIterable.walkFiles(directory)) {
					Path dest = dir.resolve(directory.relativize(path).toString());
					U.createDirs(dest);
					Files.copy(path, dest);
				}
			}
		} catch(Exception e) {
			throw U.rethrow(e);
		}
	}

	public static void clearDirectory(Path temp) throws IOException {
		if(Files.exists(temp)) {
			Files.walk(temp)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}
}
