package io.github.astrarre.amalgamation.gradle.dependencies.remap.unpick;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import daomephsta.unpick.api.constantmappers.IConstantMapper;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator;

import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;

class TrConstantMapper implements IConstantMapper {
	final TrEnvironment environment;
	final Map<String, UnpickExtension.Method> methods;
	final Map<String, String> namedToIntermediary;
	final UnpickExtension extension;

	TrConstantMapper(UnpickExtension extension, TrEnvironment environment, Map<String, String> named) {
		this.extension = extension;
		this.environment = environment;
		this.methods = extension.getMethods();
		this.namedToIntermediary = named;
	}

	@Override
	public boolean targets(String methodOwner, String methodName, String methodDescriptor) {
		UnpickExtension.Method method = methods.get(methodName + methodDescriptor);
		if(method == null) {
			return false;
		}
		if(method.declarator().equals(methodDescriptor)) {
			return true;
		}
		String declarator = method.declarator();
		TrClass a = environment.getClass(namedToIntermediary.getOrDefault(declarator, declarator));
		if(a == null) {
			return false;
		}
		String intermediary = namedToIntermediary.getOrDefault(methodOwner, methodOwner);
		TrClass b = environment.getClass(intermediary);
		if(b == null) {
			return false;
		}
		return a.isAssignableFrom(b);
	}

	@Override
	public boolean targetsParameter(String methodOwner, String methodName, String methodDescriptor, int parameterIndex) {
		var targetMethod = methods.get(methodName + methodDescriptor);
		return targetMethod.mapping().hasParameterConstantGroup(parameterIndex);
	}

	public String getParameterConstantGroup(String methodOwner, String methodName, String methodDescriptor, int parameterIndex) {
		UnpickExtension.Method targetMethod = methods.get(methodName + methodDescriptor);
		return targetMethod.mapping().getParameterConstantGroup(parameterIndex);
	}

	@Override
	public void mapParameter(String methodOwner,
			String methodName,
			String methodDescriptor,
			int parameterIndex,
			ReplacementInstructionGenerator.Context context) {
		String constantGroupID = getParameterConstantGroup(methodOwner, methodName, methodDescriptor, parameterIndex);
		ReplacementInstructionGenerator constantGroup = extension.constantGroups().get(constantGroupID);
		if(constantGroup == null) {
			throw new IllegalStateException(String.format("The constant group '%s' does not exist. Target: %s.%s%s parameter %d",
					constantGroupID,
					methodOwner,
					methodName,
					methodDescriptor,
					parameterIndex
			));
		}
		synchronized(constantGroup) {
			if(!constantGroup.canReplace(context)) {
				context.getLogger().log(Level.INFO, "Transformation skipped. Constant group '%s' cannot transform this invocation.", constantGroupID);
				return;
			}

			constantGroup.generateReplacements(context);
		}
		context.getLogger().log(Level.INFO, "Transformation complete");
	}

	@Override
	public boolean targetsReturn(String methodOwner, String methodName, String methodDescriptor) {
		UnpickExtension.Method targetMethod = methods.get(methodName + methodDescriptor);
		return targetMethod.mapping().hasReturnConstantGroup();
	}

	public String getReturnConstantGroup(String methodOwner, String methodName, String methodDescriptor) {
		var targetMethod = methods.get(methodName + methodDescriptor);
		return targetMethod.mapping().getReturnConstantGroup();
	}

	@Override
	public void mapReturn(String methodOwner, String methodName, String methodDescriptor, ReplacementInstructionGenerator.Context context) {
		String constantGroupID = getReturnConstantGroup(methodOwner, methodName, methodDescriptor);
		ReplacementInstructionGenerator constantGroup = extension.constantGroups().get(constantGroupID);
		if(constantGroup == null) {
			throw new IllegalStateException(String.format("The constant group '%s' does not exist. Target: %s.%s%s returns",
					constantGroupID,
					methodOwner,
					methodName,
					methodDescriptor
			));
		}
		if(!constantGroup.canReplace(context)) {
			context.getLogger().log(Level.INFO, "Transformation skipped. Constant group '%s' cannot transform this invocation.", constantGroupID);
			return;
		}

		constantGroup.generateReplacements(context);
		context.getLogger().log(Level.INFO, "Transformation complete");
	}
}
