package io.github.astrarre.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.astrarre.Classes;
import org.objectweb.asm.tree.AnnotationNode;

public class PlatformId implements Identified {
	public final List<String> names;

	public PlatformId(List<String> names) {
		this.names = Collections.unmodifiableList(names);
	}

	public static PlatformId of(String... names) {
		return new PlatformId(Arrays.asList(names));
	}

	public AnnotationNode createAnnotation() {
		AnnotationNode node = new AnnotationNode(Classes.PLATFORM_DESC);
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