package io.github.astrarre.splitter.impl;

import java.util.Map;

import io.github.astrarre.api.PlatformId;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.util.SplitterUtil;
import org.objectweb.asm.tree.ClassNode;

public class ClassSplitter extends Splitter {
	public ClassSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		if(input.invisibleAnnotations != null) {
			if(!SplitterUtil.matches(input.invisibleAnnotations, forPlatform)) {
				return true;
			}
			target.invisibleAnnotations = SplitterUtil.stripAnnotations(input.invisibleAnnotations, forPlatform);
		}
		return false;
	}
}
