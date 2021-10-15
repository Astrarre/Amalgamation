package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import net.devtech.zipio.impl.util.U;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.gradle.api.file.FileCollection;

public abstract class RemapSourcesJar extends RemapJar {
	@Override
	public void remap() throws IOException {
		var set = Mappings.fromLorenz(this.getMappings().get().toPath(), this.getFrom().get(), this.getTo().get());
		Mercury mercury = RemapTask.createMercury(set, this.getClasspath());

		Path current = this.getCurrent();
		Path temp = this.getProject().getBuildDir().toPath().resolve("amalg-remap-sources-temp");
		try {
			this.clearDirectory(temp);

			try(FileSystem system = U.openZip(current)) {
				for(Path directory : system.getRootDirectories()) {
					for(Path path : UnsafeIterable.walkFiles(directory)) {
						Path dest = temp.resolve(directory.relativize(path).toString());
						Files.createDirectories(dest.getParent());
						Files.copy(path, dest);
					}
				}
			} catch(Exception e) {
				throw U.rethrow(e);
			}

			try(FileSystem system = U.createZip(current)) {
				mercury.rewrite(temp, system.getPath("/"));
			} catch(Exception e) {
				throw U.rethrow(e);
			}
		} finally {
			this.clearDirectory(temp);
		}
	}

	public void clearDirectory(Path temp) throws IOException {
		if(Files.exists(temp)) {
			Files.walk(temp)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}
}
