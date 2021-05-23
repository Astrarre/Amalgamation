package io.github.astrarre.amalgamation.gradle.platform.annotationHandler;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.platform.api.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;

public class EnvironmentAnnotationHandler implements AnnotationHandler {
	public static final EnvironmentAnnotationHandler INSTANCE = new EnvironmentAnnotationHandler();

	private static final List<String> CLIENT = Collections.singletonList("client"), SERVER = Collections.singletonList("server");

	@Override
	public @Nullable List<String> expand(AnnotationNode node) {
		if(Constants.ENVIRONMENT.equals(node.desc)) {
			String[] enumEntry = MergeUtil.get(node, "value", null);
			if(enumEntry == null) return null;
			else if(enumEntry[1].equals("CLIENT")) {
				return CLIENT;
			} else {
				return SERVER;
			}
		}
		return null;
	}

	@Override
	public @Nullable AnnotationNode condense(PlatformId names) {
		if(Iterables.elementsEqual(CLIENT, names.names)) {
			AnnotationNode node = new AnnotationNode(Constants.ENVIRONMENT);
			node.visitEnum("value", Constants.ENV_TYPE, "CLIENT");
			return node;
		} else if(Iterables.elementsEqual(SERVER, names.names)) {
			AnnotationNode node = new AnnotationNode(Constants.ENVIRONMENT);
			node.visitEnum("value", Constants.ENV_TYPE, "SERVER");
			return node;
		}
		return null;
	}
}
