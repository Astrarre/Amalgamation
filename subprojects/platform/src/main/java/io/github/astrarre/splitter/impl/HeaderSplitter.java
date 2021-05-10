package io.github.astrarre.splitter.impl;

import java.util.Map;

import io.github.astrarre.api.PlatformId;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.util.SplitterUtil;
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
