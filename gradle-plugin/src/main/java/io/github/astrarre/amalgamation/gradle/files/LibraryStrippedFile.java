package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;

public class LibraryStrippedFile extends ZipProcessCachedFile {
	private final CachedFile serverJar;

	public LibraryStrippedFile(Project project, Path file, CachedFile serverJar) {
		super(file, project);
		this.serverJar = serverJar;
	}

	@Override
	public void init(ZipProcessBuilder process, Path outputFile) throws IOException {
		process.setEntryProcessor(buffer -> {
			String name = buffer.path();
			if(!name.endsWith(".class") || // copy non-classes
			   !name.contains("/") || // copy root dir files
			   name.startsWith("/") && !name.substring(1).contains("/") || // copy root dir files
			   name.contains("net/minecraft")
			) {
				buffer.copyToOutput();
			}
			return ProcessResult.HANDLED;
		});
		process.addZip(this.serverJar.getOutput(), outputFile);
	}

	@Override
	public void hashInputs(Hasher hasher) {
		this.serverJar.hashInputs(hasher);
	}
}
