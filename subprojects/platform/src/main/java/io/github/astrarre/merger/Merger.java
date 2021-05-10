package io.github.astrarre.merger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.merger.api.PlatformId;
import io.github.astrarre.merger.api.Platformed;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public abstract class Merger implements Opcodes {

	public Merger(Map<String, ?> properties) {}

	/**
	 * todo optimize, if there is only one class being inputted, and the jar is a non-merged jar, then you just need to annotate the header (class merger only)
	 * @param allActivePlatforms every possible platform combination that may be encountered
	 */
	public abstract void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations);

}
