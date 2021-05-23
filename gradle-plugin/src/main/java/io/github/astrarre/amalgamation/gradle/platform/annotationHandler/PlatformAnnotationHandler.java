package io.github.astrarre.amalgamation.gradle.platform.annotationHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public class PlatformAnnotationHandler implements AnnotationHandler, InterfaceAnnotationHandler {
	public static final PlatformAnnotationHandler INSTANCE = new PlatformAnnotationHandler();
	@Override
	public @Nullable List<String> expand(AnnotationNode node) {
		if(Constants.PLATFORM_DESC.equals(node.desc)) {
			return MergeUtil.get(node, "value", Collections.emptyList());
		}
		return null;
	}

	@Override
	public @Nullable AnnotationNode condense(PlatformId names) {
		AnnotationNode condensed = new AnnotationNode(Constants.PLATFORM_DESC);
		condensed.values = new ArrayList<>();
		condensed.values.add("value");
		condensed.values.add(names.names);
		return condensed;
	}

	@Override
	public @Nullable AnnotationNode create(Collection<PlatformId> platform, String interfaceName) {
		List<AnnotationNode> nodes = new ArrayList<>();
		platform.forEach(id -> nodes.add(id.createAnnotation(MergeUtil.ONLY_PLATFORM))); // todo reducing
		AnnotationNode interfaceAnnotation = new AnnotationNode(Constants.INTERFACE_DESC);
		interfaceAnnotation.visit("parent", Type.getObjectType(interfaceName));
		interfaceAnnotation.visit("platforms", nodes);
		return interfaceAnnotation;
	}

	@Override
	public @Nullable SidedInterface parseInterface(AnnotationNode node) {
		if(Constants.INTERFACE_DESC.equals(node.desc)) {
			Type type = MergeUtil.get(node, "parent", Constants.OBJECT_TYPE);
			List<AnnotationNode> platforms = MergeUtil.get(node, "platforms", Collections.emptyList());
			return new SidedInterface(Platformed.getPlatforms(MergeUtil.ONLY_PLATFORM, platforms, PlatformId.EMPTY), type.getInternalName());
		}
		return null;
	}
}
