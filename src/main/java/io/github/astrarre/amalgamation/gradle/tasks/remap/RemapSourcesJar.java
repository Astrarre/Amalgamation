package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import net.devtech.filepipeline.impl.util.FPInternal;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;

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
			try(FileSystem system = FileSystems.newFileSystem(current, Map.of("create", "true"))) {
				mercury.rewrite(temp, system.getPath("/"));
			} catch(Exception e) {
				e.printStackTrace();
			}
		} finally {
			clearDirectory(temp);
		}
	}

	public static void dumpZip(Path zip, Path dir) {
		try(FileSystem system = FileSystems.newFileSystem(zip, Map.of("create", "true"))) {
			for(Path directory : system.getRootDirectories()) {
				for(Path path : UnsafeIterable.walkFiles(directory)) {
					Path dest = dir.resolve(directory.relativize(path).toString());
					Files.createDirectories(dest.getParent());
					Files.copy(path, dest);
				}
			}
		} catch(Exception e) {
			throw FPInternal.rethrow(e);
		}
	}

	public static void clearDirectory(Path temp) throws IOException {
		if(Files.exists(temp)) {
			for(Path path : UnsafeIterable.walkAll(temp)) {
				Files.delete(path);
			}
		}
	}
}
