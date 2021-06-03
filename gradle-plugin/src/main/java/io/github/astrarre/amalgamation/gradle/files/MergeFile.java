package io.github.astrarre.amalgamation.gradle.files;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.github.astrarre.amalgamation.gradle.dependencies.MergerDependency;
import io.github.astrarre.amalgamation.gradle.platform.merger.MergerConfig;
import io.github.astrarre.amalgamation.gradle.platform.merger.MergerFile;
import io.github.astrarre.amalgamation.gradle.utils.AmalgamationIO;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

public class MergeFile extends CachedFile<Void> {
	private final Project project;
	private final MergerDependency dep;

	public MergeFile(MergerDependency dependency, Project project) {
		super(() -> (dependency.globalCache ? AmalgamationIO.globalCache(dependency.project.getGradle()) :
		             AmalgamationIO.projectCache(dependency.project))
				            .resolve("merges")
				            .resolve(dependency.getName()), Void.class);
		this.dep = dependency;
		this.project = project;
	}

	@Nullable
	@Override
	protected Void writeIfOutdated(Path path, @Nullable Void currentData) throws Throwable {
		if (Files.exists(path)) {
			return null;
		}
		this.project.getLogger().lifecycle("Merging " + (this.dep.unique.size() + this.dep.merge.size()) + " dependencies");
		try (Clock ignored = new Clock("Merged " + (this.dep.unique.size() + this.dep.merge.size()) + " dependencies in %dms",
				this.project.getLogger())) {
			Map<String, Object> config = new HashMap<>();
			config.put("compareInstructions", this.dep.compareInstructions);

			Map<String, List<String>> combinationData = new HashMap<>();
			for (MergerDependency.TypeEntry entry : this.dep.additionalEntries) {
				combinationData.computeIfAbsent(entry.type, s -> new ArrayList<>()).add(entry.entry);
			}

			Map<Set<String>, Iterable<File>> toMerge = new HashMap<>();
			for (Map.Entry<List<MergerDependency.TypeEntry>, Iterable<File>> entry : Iterables.concat(this.dep.uniqueResolved.entrySet(), this.dep.mergeResolved.entrySet())) {
				toMerge.put(entry.getKey().stream().map(t -> t.entry).collect(Collectors.toCollection(ListOrderedSet::new)), entry.getValue());
			}

			MergerConfig cfg = new MergerConfig();
			cfg.addAllMergers(MergerConfig.defaults(config));
			cfg.combinationsData(combinationData);
			toMerge.forEach((strings, files) -> {
				for (File file : files) {
					cfg.addInput(strings, new MergerFile().addInputZip(file.toPath()));
				}
			});

			Files.createDirectories(path);
			cfg.merge(path.resolve("merged.jar"), false, true);
		}
		return null;
	}
}
