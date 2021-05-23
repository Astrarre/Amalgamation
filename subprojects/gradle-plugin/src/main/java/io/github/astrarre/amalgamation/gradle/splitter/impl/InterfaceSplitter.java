package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.splitter.Splitter;
import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.merger.util.AsmUtil;
import io.github.astrarre.amalgamation.gradle.splitter.util.SplitterUtil;
import io.github.astrarre.amalgamation.gradle.utils.Classes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceSplitter extends Splitter {
	public InterfaceSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		target.interfaces = new ArrayList<>(input.interfaces);
		if (input.invisibleAnnotations != null) {
			for (AnnotationNode annotation : input.invisibleAnnotations) {
				if(Classes.INTERFACE_DESC.equals(annotation.desc)) {
					Type type = AsmUtil.get(annotation, "parent", Classes.OBJECT_TYPE);
					List<AnnotationNode> platforms = AsmUtil.get(annotation, "platforms", Collections.emptyList());
					if(!SplitterUtil.matches(platforms, forPlatform)) {
						target.interfaces.remove(type.getInternalName());
					}
				}
			}
		}

		return false;
	}
}
