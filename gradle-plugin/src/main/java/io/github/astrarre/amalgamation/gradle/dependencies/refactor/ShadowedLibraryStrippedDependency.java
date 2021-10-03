package io.github.astrarre.amalgamation.gradle.dependencies.refactor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;

public class ShadowedLibraryStrippedDependency extends ZipProcessDependency {
	// todo check if start / is there or not
	public final Path destinationPath;
	public boolean shouldOutput = true;
	public Object toStrip;
	public String unobfPackage = "net/minecraft";

	public ShadowedLibraryStrippedDependency(Project project, String version, Path path) {
		super(project, "io.github.amalgamation", "stripped-dependency", version);
		this.destinationPath = path;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.toStrip = this.hashDep(hasher, this.toStrip);
	}

	@Override
	protected Path evaluatePath(byte[] hash) {
		return this.destinationPath;
	}

	@Override
	protected void add(ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			process.setEntryProcessor(buffer -> {
				String name = buffer.path();
				if(!name.endsWith(".class") || // copy non-classes
				   !name.contains("/") || // copy root dir files
				   name.startsWith("/") && !name.substring(1).contains("/") || // copy root dir files
				   name.contains(this.unobfPackage)) {
					buffer.copyToOutput();
				}
				return ProcessResult.HANDLED;
			});
			ZipProcessable.add(this.project, process, this.of(this.toStrip), p -> new OutputTag(resolvedPath));
		} else {
			process.addProcessed(resolvedPath);
		}
	}
}
