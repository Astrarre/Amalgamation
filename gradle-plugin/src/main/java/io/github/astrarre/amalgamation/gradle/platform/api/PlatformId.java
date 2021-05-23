package io.github.astrarre.amalgamation.gradle.platform.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import org.objectweb.asm.tree.AnnotationNode;

public class PlatformId implements Identified {
	public static final PlatformId EMPTY = of();
	public final List<String> names;

	public PlatformId(List<String> names) {
		this.names = Collections.unmodifiableList(names);
	}

	public static PlatformId of(String... names) {
		return new PlatformId(Arrays.asList(names));
	}

	public AnnotationNode createAnnotation(List<AnnotationHandler> annotationHandlers) {
		for (AnnotationHandler handler : annotationHandlers) {
			AnnotationNode node = handler.condense(this);
			if(node != null) {
				return node;
			}
		}
		throw new IllegalStateException("no handler found for " + this);
	}

	@Override
	public PlatformId get() {
		return this;
	}

	@Override
	public String toString() {
		return this.names.toString();
	}
}
