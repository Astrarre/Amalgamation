package io.github.astrarre.splitter;

import java.util.Map;

import io.github.astrarre.api.PlatformId;
import org.objectweb.asm.tree.ClassNode;

public abstract class Splitter {
	public Splitter(Map<String, ?> properties) {}

	public abstract void split(ClassNode input, PlatformId forPlatform, ClassNode target);
}
