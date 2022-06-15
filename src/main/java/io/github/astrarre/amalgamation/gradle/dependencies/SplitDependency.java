package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.func.UCons;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import io.github.astrarre.amalgamation.gradle.utils.zip.ZipIO;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class SplitDependency extends CachedDependency {
	public final Dependency dependency;
	public Path outputDir;
	
	public SplitDependency(Project project, Dependency dependency) {
		super(project);
		this.dependency = dependency;
	}
	
	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.dependency);
	}
	
	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return this.outputDir;
	}
	
	@Override
	protected Set<Artifact> resolve0(Path resolvedPath, boolean isOutdated) throws Exception {
		Artifact cls = this.artifact(resolvedPath.resolve("classes.jar"), Artifact.Type.MIXED);
		Artifact rss = this.artifact(resolvedPath.resolve("resources.jar"), Artifact.Type.RESOURCES);
		Artifact src = this.artifact(resolvedPath.resolve("sources.jar"), Artifact.Type.SOURCES);
		if(isOutdated) {
			if(!Files.exists(resolvedPath)) {
				Files.createDirectories(resolvedPath);
			}
			Files.write(resolvedPath.resolve("resources.jar.rss_marker"), new byte[] {});
			try(FSRef classes = ZipIO.createZip(cls.file); FSRef resources = ZipIO.createZip(rss.file); FSRef sources = ZipIO.createZip(src.file)) {
				for(Artifact artifact : this.artifacts(this.dependency)) {
					FSRef input = ZipIO.readZip(artifact.file);
					input.walk().filter(Files::isRegularFile).forEach(UCons.of(from -> {
						String path = from.toString();
						FSRef ref;
						if(path.endsWith(".class") || path.contains("META-INF")) {
							ref = classes;
						} else if(path.endsWith(".java")) {
							ref = sources;
						} else {
							ref = resources;
						}
						Path path1 = ref.getPath(path);
						AmalgIO.createParent(path1); // todo this can be optimized with a better directorizing stream
						Files.copy(from, path1);
					}));
				}
			}
		}
		return Set.of(cls, rss, src);
	}
	
	Artifact.File artifact(Path dest, Artifact.Type type) {
		return new Artifact.File(
				this.project,
				this.dependency.getGroup(),
				this.dependency.getName() + type.classifier,
				this.dependency.getVersion(),
				dest,
				this.getCurrentHash(),
				type
		);
	}
	
}
