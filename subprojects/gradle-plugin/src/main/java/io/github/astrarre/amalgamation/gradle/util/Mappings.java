package io.github.astrarre.amalgamation.gradle.util;

import java.util.function.Consumer;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

import net.fabricmc.tinyremapper.IMappingProvider;

public class Mappings {
	public static void iterateClasses(MappingSet mappings, Consumer<ClassMapping<?, ?>> consumer) {
		for (TopLevelClassMapping topLevelClassMapping : mappings.getTopLevelClassMappings()) {
			iterateClasses(topLevelClassMapping, consumer);
		}
	}

	public static IMappingProvider createMappingProvider(MappingSet mappings) {
		return out -> iterateClasses(mappings, classMapping -> {
			String owner = classMapping.getFullObfuscatedName();
			out.acceptClass(owner, classMapping.getFullDeobfuscatedName());

			for (MethodMapping methodMapping : classMapping.getMethodMappings()) {
				out.acceptMethod(new IMappingProvider.Member(owner, methodMapping.getObfuscatedName(), methodMapping.getObfuscatedDescriptor()),
						methodMapping.getDeobfuscatedName());
			}

			for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
				fieldMapping.getType()
				            .ifPresent(fieldType -> out.acceptField(new IMappingProvider.Member(owner,
						            fieldMapping.getObfuscatedName(),
						            fieldType.toString()), fieldMapping.getDeobfuscatedName()));
			}
		});
	}

	private static void iterateClasses(ClassMapping<?, ?> classMapping, Consumer<ClassMapping<?, ?>> consumer) {
		consumer.accept(classMapping);

		for (InnerClassMapping innerClassMapping : classMapping.getInnerClassMappings()) {
			iterateClasses(innerClassMapping, consumer);
		}
	}
}
