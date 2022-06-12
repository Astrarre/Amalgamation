package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.ide.TaskConverter;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.impl.util.FPInternal;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;

/**
 * An interesting hack, creates a jar file with manifest file with the classpath in it. This allows for really large classpaths
 */
public class ManifestJarDependency extends CachedDependency {
	final String path;
	final List<String> files;

	public ManifestJarDependency(Project project, String path, JavaExec files) {
		super(project);
		this.path = path.replaceAll("[^A-Za-z0-9]", "_");
		this.files = TaskConverter.getClasspath(files);
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		for(String file : files) {
			hasher.putString(file, StandardCharsets.UTF_8);
		}
	}

	@Override
	protected VirtualPath evaluatePath(byte[] hash) throws IOException {
		return AmalgDirs.PROJECT.root(this.project).getDir("classpath_manifests").getFile(this.path + ".jar");
	}

	@Override
	protected Set<Artifact> resolve0(VirtualPath resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			try(VirtualSink sink = AmalgIO.DISK_OUT.subsink(resolvedPath)) {
				VirtualFile path = sink.outputFile("META-INF/MANIFEST.MF");
				String cp = String.join(" ", this.files);
				sink.writeString(path, String.format("Class-Path: %s", String.join(" ", cp)), StandardCharsets.UTF_8);
			} catch(Exception e) {
				throw FPInternal.rethrow(e);
			}
		}
		return Set.of(new Artifact.File(
				this.project,
				"io.github.astrarre.amalgamation",
				"manifest",
				"0",
				resolvedPath,
				this.getCurrentHash(),
				Artifact.Type.RESOURCES
		));
	}
}
