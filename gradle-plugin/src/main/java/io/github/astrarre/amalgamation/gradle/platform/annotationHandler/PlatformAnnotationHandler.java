package io.github.astrarre.amalgamation.gradle.platform.annotationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;

public class PlatformAnnotationHandler implements AnnotationHandler {
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
}
