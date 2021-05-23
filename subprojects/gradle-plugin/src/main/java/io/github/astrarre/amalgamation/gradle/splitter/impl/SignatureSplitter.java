package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.Map;

import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class SignatureSplitter extends Splitter {
	public SignatureSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		target.signature = input.signature; // todo intelligent signature merging
		return false;
	}
}
