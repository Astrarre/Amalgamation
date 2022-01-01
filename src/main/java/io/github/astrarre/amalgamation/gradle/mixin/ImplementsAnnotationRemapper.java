package io.github.astrarre.amalgamation.gradle.mixin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class ImplementsAnnotationRemapper extends AnnotationVisitor {
	public final MixinClass state;

	public ImplementsAnnotationRemapper(int api, AnnotationVisitor annotationVisitor, MixinClass state) {
		super(api, annotationVisitor);
		this.state = state;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String descriptor) {
		AnnotationVisitor visitor = super.visitAnnotation(name, descriptor);
		if(descriptor.equals("Lorg/spongepowered/asm/mixin/Interface;")) {
			return new InterfaceAnnotationRemapper(visitor);
		} else {
			return visitor;
		}
	}

	class InterfaceAnnotationRemapper extends AnnotationVisitor {
		Type iface;
		String prefix;

		public InterfaceAnnotationRemapper(AnnotationVisitor annotationVisitor) {
			super(ImplementsAnnotationRemapper.this.api, annotationVisitor);
		}

		@Override
		public void visit(String name, Object value) {
			if(name.equals("prefix")) {
				this.prefix = (String) value;
			} else if(name.equals("iface")) {
				this.iface = (Type) value;
			}
			super.visit(name, value);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			ImplementsAnnotationRemapper.this.state.prefixes.add(
					new MixinClass.Implements(
							this.prefix, this.iface.getInternalName()
					)
			);
		}
	}
}
