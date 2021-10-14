package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.filtr.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class SingleRemapDependency extends ZipProcessDependency {
	final byte[] mappingsHash;
	final Dependency delegate;
	final boolean global;
	final boolean isClasspath;
	final AmalgRemapper remapper;

	public SingleRemapDependency(Project project, AmalgRemapper remapper, byte[] hash, Dependency delegate, boolean global, boolean isClasspath) {
		super(project, "io.github.astrarre.amalgamation.gradle", "remap_single", "-1");
		this.remapper = remapper;
		this.mappingsHash = hash;
		this.delegate = delegate;
		this.global = global;
		this.isClasspath = isClasspath;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.hashDep(hasher, this.delegate);
		hasher.putBytes(this.mappingsHash);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return AmalgIO.cache(this.project, global).resolve("remaps").resolve(AmalgIO.b64(hash));
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		int[] ur = {0};
		UnaryOperator<OutputTag> operator;
		if(!isOutdated && this.isClasspath) {
			operator = p -> OutputTag.INPUT;
		} else {
			operator = p -> tag(p, resolvedPath.resolve(AmalgIO.insertName(p.getVirtualPath(), "_" + ur[0]++)));
		}

		var add = resolver.apply(this.delegate, operator);
		if(isOutdated) {
			Files.createDirectories(resolvedPath);
			for(Path path : UnsafeIterable.walkFiles(resolvedPath)) {
				Files.deleteIfExists(path);
			}

			for(var transform : add) {
				Map<OutputTag, AmalgRemapper.Remap> remapMap = new HashMap<>();
				transform.setPreEntryProcessor(o -> remapMap.computeIfAbsent(o, a -> this.remapper.remap()));
				transform.setPostZipProcessor(o -> remapMap.computeIfAbsent(o, a -> this.remapper.remap()));
			}
		} else {
			for(Path path : UnsafeIterable.walkFiles(resolvedPath)) {
				process.addProcessed(path.toAbsolutePath());
			}
			for(TaskTransform transform : add) {
				if(this.isClasspath) {
					transform.setPreEntryProcessor(o -> this.remapper.classpath());
				} else {
					transform.setZipFilter(o -> ResourceZipFilter.INVERTED);
				}
			}
		}
	}
}
