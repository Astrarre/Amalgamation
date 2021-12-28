package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.gradle.api.Project;

public class ShadowedLibraryStrippedDependency extends ZipProcessDependency {
	public final Path destinationPath;
	public boolean shouldOutput = true; // todo impl
	public Object toStrip;
	public String unobfPackage = "net/minecraft";
	public String version = "NaN";

	public ShadowedLibraryStrippedDependency(Project project, Path path) {
		super(project);
		this.destinationPath = path;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.toStrip);
	}

	@Override
	protected Path evaluatePath(byte[] hash) {
		return this.destinationPath;
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		Artifact.File file = new Artifact.File(
				this.project,
				"net.minecraft",
				"stripped",
				this.version,
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.MIXED
		);
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
			resolver.apply(this.of(this.toStrip), p -> file);
		} else {
			process.addProcessed(file);
		}
	}
}
