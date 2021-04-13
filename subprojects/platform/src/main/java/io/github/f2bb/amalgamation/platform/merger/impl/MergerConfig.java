package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.ToIntFunction;

import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.tree.ClassNode;

public class MergerConfig {
	private final Collection<PlatformData> available;
	public final boolean compareInstructions;

	public MergerConfig(Collection<PlatformData> available, boolean instructions) {
		this.available = available;
		this.compareInstructions = instructions;
	}

	public Collection<PlatformData> getAvailable() {
		return this.available;
	}
}
