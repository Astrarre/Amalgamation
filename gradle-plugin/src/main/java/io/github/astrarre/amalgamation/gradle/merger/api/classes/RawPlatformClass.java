package io.github.astrarre.amalgamation.gradle.merger.api.classes;

import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.merger.api.Platformed;
import io.github.astrarre.amalgamation.gradle.merger.api.classpath.Context;
import org.objectweb.asm.tree.ClassNode;

public class RawPlatformClass extends Platformed<ClassNode> {
	public final Context context;
	public RawPlatformClass(PlatformId platforms, ClassNode val, Context context) {
		super(platforms, val);
		this.context = context;
	}
}
