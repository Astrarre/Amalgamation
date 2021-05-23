package io.github.astrarre.amalgamation.gradle.merger.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.utils.Constants;
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

	public AnnotationNode createAnnotation() {
		AnnotationNode node = new AnnotationNode(Constants.PLATFORM_DESC);
		node.visit("value", this.names);
		return node;
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
