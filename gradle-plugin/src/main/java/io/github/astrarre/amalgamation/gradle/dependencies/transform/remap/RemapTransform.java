package io.github.astrarre.amalgamation.gradle.dependencies.transform.remap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.filters.ResourceZipFilter;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.AbstractBinRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.remapper.bin.TRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.TransformDependency;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class RemapTransform implements TransformDependency.Transformer<RemapHelper> {
	final List<MappingTarget> targets = new ArrayList<>();
	AmalgRemapper remapper = new TRemapper(), srcRemapper;

	public void setRemapper(AmalgRemapper remapper) {
		this.remapper = remapper;
		if(this.srcRemapper != null) {
			this.setSrcRemapper(remapper);
		}
	}

	public void setSrcRemapper(AmalgRemapper remapper) {
		if(this.remapper instanceof AbstractBinRemapper a) {
			a.setSourceRemapper(remapper);
			this.srcRemapper = remapper;
		} else {
			throw new UnsupportedOperationException("cannot set source remapper on non-AbstractRemapper remapper!");
		}
	}

	@Override
	public Class<RemapHelper> configurationHelper() {
		return RemapHelper.class;
	}

	public void addMappings(MappingTarget target) {
		this.targets.add(target);
	}

	@Override
	public void configure(Project project, TransformDependency.Inputs inputs, ZipProcessBuilder builder) throws IOException {
		List<Mappings.Namespaced> maps = new ArrayList<>();
		for(var mapping : this.targets) {
			maps.add(mapping.read());
		}
		this.remapper.init(maps);
		Logger logger = project.getLogger();
		logger.lifecycle("Remapping " + (inputs.get(RemapHelper.INPUT_GLOBAL).size() + inputs.get(RemapHelper.INPUT_LOCAL).size()) + " dependencies");
		for(TransformDependency.Input<Void> input : inputs.get(RemapHelper.CLASSPATH)) {
			for(TaskTransform task : input.appendInputs(builder, tag -> OutputTag.INPUT)) {
				task.setPreEntryProcessor(o -> this.remapper.classpath());
				task.setZipFilter(o -> ResourceZipFilter.SKIP);
			}
		}

		for(var input : inputs.getAll(RemapHelper.INPUT_GLOBAL, RemapHelper.INPUT_LOCAL)) {
			for(TaskTransform task : input.appendInputs(builder)) {
				Map<OutputTag, AmalgRemapper.Remap> remapMap = new HashMap<>();
				task.setPreEntryProcessor(o -> remapMap.computeIfAbsent(o, a -> this.remapper.remap()));
				task.setPostZipProcessor(o -> remapMap.computeIfAbsent(o, a -> this.remapper.remap()));
			}
		}
	}

	@Override
	public void hash(Hasher hasher) throws IOException {
		for(MappingTarget target : this.targets) {
			target.hash(hasher);
		}
		this.remapper.hash(hasher);
	}
}
