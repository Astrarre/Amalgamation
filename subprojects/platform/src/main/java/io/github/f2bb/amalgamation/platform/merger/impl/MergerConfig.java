package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.ToIntFunction;

import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.tree.ClassNode;

public class MergerConfig {
	private final ClassNode node = new ClassNode();
	private final List<ClassInfo> infos;
	private final Collection<PlatformData> available;
	private final ToIntFunction<String> versionIndex;
	public final boolean compareInstructions;

	public MergerConfig(List<ClassInfo> infos, Collection<PlatformData> available, ToIntFunction<String> index, boolean instructions) {
		this.infos = infos;
		this.available = available;
		this.versionIndex = index;
		this.compareInstructions = instructions;
	}

	public ClassNode getNode() {
		return this.node;
	}

	public List<ClassInfo> getInfos() {
		return this.infos;
	}

	public Collection<PlatformData> getAvailable() {
		return this.available;
	}

	/**
	 * @return an 'index' of this version, if the version is new, the number is small, if it is old, it is big
	 */
	public int getIndex(String version) {
		return this.versionIndex.applyAsInt(version);
	}
}
