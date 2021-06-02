package io.github.astrarre.amalgamation.gradle.platform.splitter;

import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public abstract class Splitter implements Opcodes {
	public Splitter(Map<String, ?> properties) {}

	/**
	 * @return true if the class should be skipped
	 */
	public abstract boolean split(ClassNode input, PlatformId forPlatform, ClassNode target, AnnotationHandler handler);
}
