package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.Map;

import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.objectweb.asm.tree.ClassNode;

public class ClassSplitter extends Splitter {
	public ClassSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		if(input.invisibleAnnotations != null) {
			if(!MergeUtil.matches(input.invisibleAnnotations, forPlatform)) {
				return true;
			}
			target.invisibleAnnotations = MergeUtil.stripAnnotations(input.invisibleAnnotations, forPlatform);
		}
		return false;
	}
}
