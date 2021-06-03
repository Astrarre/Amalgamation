package io.github.astrarre.amalgamation.gradle.platform.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.objectweb.asm.tree.AnnotationNode;

public class PlatformId implements Identified {
	public static final PlatformId EMPTY = of();
	public final Set<String> names;

	public PlatformId(Set<String> names) {
		this.names = Collections.unmodifiableSet(names);
	}

	public PlatformId(PlatformId platform, Set<String> and) {
		Set<String> names = ListOrderedSet.listOrderedSet(platform.names);
		names.addAll(and);
		this.names = Collections.unmodifiableSet(names);
	}

	public static PlatformId of(String... names) {
		return new PlatformId(ListOrderedSet.listOrderedSet(Arrays.asList(names)));
	}

	public AnnotationNode createAnnotation(List<AnnotationHandler> annotationHandlers) {
		for (AnnotationHandler handler : annotationHandlers) {
			AnnotationNode node = handler.createClassPlatformAnnotation(this);
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
