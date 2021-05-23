package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.Map;

import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class SuperclassSplitter extends Splitter {
	public SuperclassSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		target.superName = input.superName; // todo parent annotation
		return false;
	}
}
