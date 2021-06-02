package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceSplitter extends Splitter {
	public InterfaceSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler handler) {
		List<String> interfaces = new ArrayList<>(input.interfaces);
		Set<String> validInterfaces = new HashSet<>();
		if (target.invisibleAnnotations != null) {
			List<AnnotationNode> annotations = target.invisibleAnnotations;
			for (int i = annotations.size() - 1; i >= 0; i--) {
				AnnotationNode annotation = annotations.get(i);
				Platformed<String> ifacePlatform = handler.parseInterfacePlatforms(annotation);
				if (ifacePlatform != null) {
					if(ifacePlatform.id.names.containsAll(forPlatform.names)) {
						// valid interface
						validInterfaces.add(ifacePlatform.val);
						List<String> newNames = new ArrayList<>(ifacePlatform.id.names);
						newNames.removeAll(forPlatform.names);
						if(!newNames.isEmpty()) {
							annotations.add(handler.createInterfacePlatformAnnotation(new Platformed<>(new PlatformId(newNames), ifacePlatform.val)));
						}
					} else {
						interfaces.remove(ifacePlatform.val);
					}
				}
			}
		}
		interfaces.addAll(validInterfaces);
		target.interfaces = interfaces;

		return false;
	}
}
