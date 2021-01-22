package io.github.f2bb.amalgamation.gradle.impl;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

import java.util.function.Consumer;

public class MappingUtils {

    public static void iterateClasses(MappingSet mappings, Consumer<ClassMapping<?, ?>> consumer) {
        for (TopLevelClassMapping topLevelClassMapping : mappings.getTopLevelClassMappings()) {
            iterateClasses(topLevelClassMapping, consumer);
        }
    }

    private static void iterateClasses(ClassMapping<?, ?> classMapping, Consumer<ClassMapping<?, ?>> consumer) {
        consumer.accept(classMapping);

        for (InnerClassMapping innerClassMapping : classMapping.getInnerClassMappings()) {
            iterateClasses(innerClassMapping, consumer);
        }
    }
}
