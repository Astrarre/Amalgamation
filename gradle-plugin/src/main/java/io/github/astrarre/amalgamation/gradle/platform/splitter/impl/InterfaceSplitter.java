package io.github.astrarre.amalgamation.gradle.platform.splitter.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.InterfaceAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.splitter.Splitter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class InterfaceSplitter extends Splitter {
	public InterfaceSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, List<AnnotationHandler> annotationHandlers) {
		target.interfaces = new ArrayList<>(input.interfaces);
		if (target.invisibleAnnotations != null) {
			List<AnnotationNode> annotations = target.invisibleAnnotations;
			for (int i = annotations.size() - 1; i >= 0; i--) {
				AnnotationNode annotation = annotations.get(i);
				boolean handledAnnotation = false;
				for (AnnotationHandler handler : annotationHandlers) {
					if (handler instanceof InterfaceAnnotationHandler) {
						InterfaceAnnotationHandler.SidedInterface instance = ((InterfaceAnnotationHandler) handler).parseInterface(annotation);
						if (instance != null) {
							handledAnnotation = true;
							boolean found = false;
							for (PlatformId platform : instance.platforms) {
								if (platform.names.containsAll(forPlatform.names)) {
									found = true;
								}
							}
							if (found) {
								Set<PlatformId> newPlatforms = new HashSet<>();
								instance.platforms.forEach(platformId -> {
									if (platformId.names.containsAll(forPlatform.names)) {
										List<String> newNames = new ArrayList<>(platformId.names);
										newNames.removeAll(forPlatform.names);
										if (!newNames.isEmpty()) {
											newPlatforms.add(new PlatformId(newNames));
										}
									}
								});
								if (!newPlatforms.isEmpty()) {
									if (target.invisibleAnnotations == null) {
										target.invisibleAnnotations = new ArrayList<>();
									}
									target.invisibleAnnotations.add(((InterfaceAnnotationHandler) handler).create(newPlatforms,
											instance.interfaceName));
								}
							} else {
								target.interfaces.remove(instance.interfaceName);
							}
							break;
						}
					}
				}
				if (!handledAnnotation) {
					target.invisibleAnnotations.add(annotation);
				} else {
					target.invisibleAnnotations.remove(i);
				}
			}
		}

		return false;
	}
}
