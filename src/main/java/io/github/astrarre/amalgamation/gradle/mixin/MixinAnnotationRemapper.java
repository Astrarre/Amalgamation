package io.github.astrarre.amalgamation.gradle.mixin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class MixinAnnotationRemapper extends AnnotationVisitor {
	public final MixinClass refmap;

	public MixinAnnotationRemapper(int api, AnnotationVisitor annotationVisitor, MixinClass refmap) {
		super(api, annotationVisitor);
		this.refmap = refmap;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, value);
		if(name.equals("remap")) {
			this.refmap.remap = (Boolean) value;
		}
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		if(name.equals("target")) {
			return new TargetVisitor(super.visitArray(name));
		} else if(name.equals("value")) {
			return new ValueVisitor(super.visitArray(name));
		} else {
			return super.visitArray(name);
		}
	}

	class TargetVisitor extends AnnotationVisitor {
		public TargetVisitor(AnnotationVisitor annotationVisitor) {
			super(MixinAnnotationRemapper.this.api, annotationVisitor);
		}

		@Override
		public void visit(String name, Object value) {
			String str = (String) value;
			String replace = str.replace('.', '/');
			MixinAnnotationRemapper.this.refmap.targets.add(replace);
			MixinAnnotationRemapper.this.refmap.applyEnvironment(env -> {
				String map = env.getRemapper().map(replace);
				MixinAnnotationRemapper.this.refmap.mappings.put(str, map);
			});
			super.visit(name, value);
		}
	}

	class ValueVisitor extends AnnotationVisitor {
		public ValueVisitor(AnnotationVisitor annotationVisitor) {
			super(MixinAnnotationRemapper.this.api, annotationVisitor);
		}

		@Override
		public void visit(String name, Object value) {
			Type str = (Type) value;
			MixinAnnotationRemapper.this.refmap.targets.add(str.getInternalName());
			super.visit(name, value);
		}
	}
}
