package io.github.astrarre.amalgamation.gradle.dependencies.remap.unpick;

import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import daomephsta.unpick.api.constantresolvers.IConstantResolver;
import daomephsta.unpick.impl.LiteralType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrRemapper;

public class ConstantResolver implements IConstantResolver {
	private static final Set<Type> VALID_CONSTANT_TYPES = Arrays.stream(LiteralType.values()).map(LiteralType::getType).collect(toSet());

	Map<String, ResolvedConstants> constantDataCache;
	final Map<String, ResolvedConstants> mapped = new ConcurrentHashMap<>();

	@Override
	public ResolvedConstant resolveConstant(String owner, String name) {
		ResolvedConstants constants = mapped.get(owner);
		if(constants == null) {
			return null;
		}
		ResolvedConstant constant = constants.get(name);
		if(constant == null) {
			return null;
		}
		return constant;
	}

	public synchronized void populateMapped(TrEnvironment environment) {
		if(constantDataCache != null) {
			TrRemapper remapper = environment.getRemapper();
			constantDataCache.forEach((s, constants) -> {
				if(constants.resolvedConstants.isEmpty()) {
					return;
				}
				ResolvedConstants copy = new ResolvedConstants(Opcodes.ASM9, null);
				constants.resolvedConstants.forEach((key, constant) -> {
					Type type = constant.getType();
					copy.resolvedConstants.put(
							remapper.mapFieldName(s, key, type.getDescriptor()),
							new ResolvedConstant(type, constant.getValue())
					);
				});
				mapped.put(
						remapper.map(s),
						copy
				);
			});
			constantDataCache = null;
		}
	}

	ClassVisitor create(String className, ClassVisitor visitor) {
		ResolvedConstants resolvedConstants = new ResolvedConstants(Opcodes.ASM9, visitor);
		if(constantDataCache == null) {
			constantDataCache = new ConcurrentHashMap<>();
		}
		constantDataCache.put(className, resolvedConstants);
		return resolvedConstants;
	}

	private static class ResolvedConstants extends ClassVisitor {
		private final Map<String, ResolvedConstant> resolvedConstants = new HashMap<>();

		public ResolvedConstants(int api, ClassVisitor classVisitor) {
			super(api, classVisitor);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if(Modifier.isStatic(access) && Modifier.isFinal(access)) {
				Type fieldType = Type.getType(descriptor);
				if(VALID_CONSTANT_TYPES.stream().anyMatch(t -> t.equals(fieldType))) {
					resolvedConstants.put(name, new ResolvedConstant(fieldType, value));
				}
			}
			return super.visitField(access, name, descriptor, signature, value);
		}



		public ResolvedConstant get(Object key) {
			return resolvedConstants.get(key);
		}
	}
}
