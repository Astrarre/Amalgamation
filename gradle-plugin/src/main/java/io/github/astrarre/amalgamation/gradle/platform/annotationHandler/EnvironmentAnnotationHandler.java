package io.github.astrarre.amalgamation.gradle.platform.annotationHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MergeUtil;
import org.gradle.internal.impldep.org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public class EnvironmentAnnotationHandler implements AnnotationHandler, InterfaceAnnotationHandler {
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

	@Override
	public @Nullable AnnotationNode create(Collection<PlatformId> platform, String interfaceName) {
		if(platform.size() == 1) {
			List<String> names = platform.iterator().next().names;
			if(Iterables.elementsEqual(names, CLIENT)) {
				AnnotationNode environmentInterface = new AnnotationNode(Constants.ENVIRONMENT_INTERFACE);
				environmentInterface.visitEnum("value", Constants.ENV_TYPE, "CLIENT");
				environmentInterface.visit("itf", Type.getObjectType(interfaceName));
				return environmentInterface;
			} else if(Iterables.elementsEqual(names, SERVER)) {
				AnnotationNode environmentInterface = new AnnotationNode(Constants.ENVIRONMENT_INTERFACE);
				environmentInterface.visitEnum("value", Constants.ENV_TYPE, "SERVER");
				environmentInterface.visit("itf", Type.getObjectType(interfaceName));
				return environmentInterface;
			}
		}
		return null;
	}

	@Override
	public @Nullable SidedInterface parseInterface(AnnotationNode node) {
		if(Constants.ENVIRONMENT_INTERFACE.equals(node.desc)) {
			String[] enumEntry = MergeUtil.get(node, "value", null);
			if(enumEntry[1].equals("CLIENT")) {
				return new SidedInterface(Collections.singletonList(new PlatformId(CLIENT)), MergeUtil.get(node, "itf", Constants.OBJECT_TYPE).getInternalName());
			} else {
				return new SidedInterface(Collections.singletonList(new PlatformId(SERVER)), MergeUtil.get(node, "itf", Constants.OBJECT_TYPE).getInternalName());
			}
		}
		return null;
	}
}
