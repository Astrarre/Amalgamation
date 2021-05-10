package io.github.astrarre.merger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;
import net.devtech.signutil.v0.api.TypeArgument;
import net.devtech.signutil.v0.api.bounded.ClassSignature;
import net.devtech.signutil.v0.api.bounded.TypeParameter;
import net.devtech.signutil.v0.api.type.reference.ClassType;
import org.objectweb.asm.tree.ClassNode;

public class SignatureMerger extends Merger {
	public SignatureMerger(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public void merge(Set<PlatformId> allActivePlatforms,
			List<Platformed<ClassNode>> inputs,
			ClassNode target,
			List<List<String>> platformCombinations) {
		Map<String, TypeParameter> typeArguments = new HashMap<>();
		ClassType superClass = ClassType.create("L" + target.superName + ";");
		Map<String, ClassType> interfaces = new HashMap<>();
		for (String anInterface : target.interfaces) {
			interfaces.put(anInterface, ClassType.create("L" + anInterface + ";"));
		}

		boolean special = false;
		for (Platformed<ClassNode> input : inputs) {
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
