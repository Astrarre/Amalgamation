package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.Map;

import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class HeaderSplitter extends Splitter {
	public HeaderSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		target.name = input.name;
		target.version = input.version;
		target.visibleAnnotations = input.visibleAnnotations;
		// todo handle versions
		return false;
	}
}
