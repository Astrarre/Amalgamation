package io.github.astrarre.amalgamation.gradle.merger.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.merger.api.classes.RawPlatformClass;

import io.github.astrarre.amalgamation.gradle.merger.Merger;
import net.devtech.signutil.v0.api.bounded.ClassSignature;
import net.devtech.signutil.v0.api.bounded.TypeParameter;
import net.devtech.signutil.v0.api.type.reference.ClassType;
import org.objectweb.asm.tree.ClassNode;

public class SignatureMerger extends Merger {
	public SignatureMerger(Map<String, ?> properties) {
		super(properties);
	}

	// todo even more advanced signature merging (eg merging type bounds)
	@Override
	public void merge(List<RawPlatformClass> inputs, ClassNode target, Map<String, List<String>> platformCombinations) {
		Map<String, TypeParameter> typeArguments = new HashMap<>();
		ClassType superClass = ClassType.create("L" + target.superName + ";");
		Map<String, ClassType> interfaces = new HashMap<>();
		for (String anInterface : target.interfaces) {
			interfaces.put(anInterface, ClassType.create("L" + anInterface + ";"));
		}

		boolean special = false;
		for (RawPlatformClass input : inputs) {
			if(input.val.signature == null) continue;
			special = true;
			ClassSignature signature = ClassSignature.create(input.val.signature);
			ClassType superType = signature.getSuperClass();
			if(superType.getInternalName().equals(target.superName)) {
				superClass = superType;
			}

			for (ClassType anInterface : signature.getInterfaces()) {
				interfaces.put(anInterface.getInternalName(), anInterface);
			}

			for (TypeParameter parameter : signature.getTypeParameters()) {
				typeArguments.put(parameter.getIdentifier(), parameter);
			}
		}

		if(special) {
			List<TypeParameter> args = new ArrayList<>(typeArguments.values());
			List<ClassType> ifaces = new ArrayList<>(interfaces.values());
			target.signature = ClassSignature.create(args, superClass, ifaces).toString();
		}
	}
}
