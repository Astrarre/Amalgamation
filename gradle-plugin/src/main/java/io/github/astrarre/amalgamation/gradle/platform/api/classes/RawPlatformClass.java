package io.github.astrarre.amalgamation.gradle.platform.api.classes;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classpath.Context;
import org.objectweb.asm.tree.ClassNode;

public class RawPlatformClass extends Platformed<ClassNode> {
	public final Context context;
	public RawPlatformClass(PlatformId platforms, ClassNode val, Context context) {
		super(platforms, val);
		this.context = context;
	}
}
