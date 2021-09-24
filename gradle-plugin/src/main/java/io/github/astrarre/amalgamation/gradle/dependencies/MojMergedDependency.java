package io.github.astrarre.amalgamation.gradle.dependencies;

import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.MojMergedFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MojMergedDependency extends AbstractSingleFileSelfResolvingDependency {
	final MojMergedFile merger;

	public MojMergedDependency(Project project, String version, CASMerger.Handler handler, Dependency clientJar, CachedFile<?> serverMappings) {
		super(project, "net.minecraft", "moj-merged", version);
		Path jar = AmalgIO.globalCache(project.getGradle()).resolve(version).resolve("moj-merged.jar");
		this.merger = new MojMergedFile(jar, project, version, handler, () -> AmalgIO.resolve(project, clientJar), serverMappings::getPath);
	}

	public MojMergedDependency(Project project, String group, String name, String version, MojMergedFile merger) {
		super(project, group, name, version);
		this.merger = merger;
	}

	@Override
	public Dependency copy() {
		return new MojMergedDependency(this.project, this.group, this.name, this.version, this.merger);
	}

	@Override
	protected Path resolvePath() {
		return this.merger.getPath();
	}
}
