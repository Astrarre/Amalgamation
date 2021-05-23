package io.github.astrarre.amalgamation.gradle.platform.annotationHandler;

import java.util.Collection;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;

public interface InterfaceAnnotationHandler {
	@Nullable
	AnnotationNode create(Collection<PlatformId> platform, String interfaceName);

	@Nullable
	SidedInterface parseInterface(AnnotationNode node);

	class SidedInterface {
		public final Iterable<PlatformId> platforms;
		public final String interfaceName;

		public SidedInterface(Iterable<PlatformId> platforms, String name) {
			this.platforms = platforms;
			this.interfaceName = name;
		}
	}
}
