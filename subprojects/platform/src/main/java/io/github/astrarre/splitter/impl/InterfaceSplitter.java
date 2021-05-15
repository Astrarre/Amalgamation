package io.github.astrarre.splitter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.astrarre.Classes;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.util.SplitterUtil;
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
