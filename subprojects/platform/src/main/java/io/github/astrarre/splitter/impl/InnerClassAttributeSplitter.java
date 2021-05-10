package io.github.astrarre.splitter.impl;

import java.util.Map;

import io.github.astrarre.api.PlatformId;
import io.github.astrarre.splitter.Splitter;
import org.objectweb.asm.tree.ClassNode;

public class InnerClassAttributeSplitter extends Splitter {
	public InnerClassAttributeSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		// todo this needs classpath
		return false;
	}
}
