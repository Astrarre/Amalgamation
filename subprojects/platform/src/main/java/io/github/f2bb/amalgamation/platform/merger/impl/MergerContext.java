package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.tree.ClassNode;

public class MergerContext {
	private final ClassNode node = new ClassNode();
	private final List<ClassInfo> infos;
	private final Collection<PlatformData> available;
	private final ToIntFunction<String> versionIndex;

	public MergerContext(List<ClassInfo> infos, Collection<PlatformData> available, ToIntFunction<String> index) {
		this.infos = infos;
		this.available = available;
		this.versionIndex = index;
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
