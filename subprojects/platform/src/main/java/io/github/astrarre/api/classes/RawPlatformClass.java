package io.github.astrarre.api.classes;

import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
import io.github.astrarre.api.classpath.Context;
import org.objectweb.asm.tree.ClassNode;

public class RawPlatformClass extends Platformed<ClassNode> {
	public final Context context;
	public RawPlatformClass(PlatformId platforms, ClassNode val, Context context) {
		super(platforms, val);
		this.context = context;
	}
}
