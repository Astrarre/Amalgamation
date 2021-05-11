package io.github.astrarre.merger;

import java.util.List;
import java.util.Map;

import io.github.astrarre.api.Platformed;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public abstract class Merger implements Opcodes {

	public Merger(Map<String, ?> properties) {}

	/**
	 * todo maybe provide classpath or something for better merging?
	 * todo optimize, if there is only one class being inputted, and the jar is a non-merged jar, then you just need to annotate the header (class merger only)
	 */
	public abstract void merge(List<Platformed<ClassNode>> inputs, ClassNode target, List<List<String>> platformCombinations);

}
