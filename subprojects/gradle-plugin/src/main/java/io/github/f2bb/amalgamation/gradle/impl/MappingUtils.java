/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.f2bb.amalgamation.gradle.impl;

import net.fabricmc.tinyremapper.IMappingProvider;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.*;

import java.util.function.Consumer;

public class MappingUtils {

    public static void iterateClasses(MappingSet mappings, Consumer<ClassMapping<?, ?>> consumer) {
        for (TopLevelClassMapping topLevelClassMapping : mappings.getTopLevelClassMappings()) {
            iterateClasses(topLevelClassMapping, consumer);
        }
    }

    public static IMappingProvider createMappingProvider(MappingSet mappings) {
        return out -> MappingUtils.iterateClasses(mappings, classMapping -> {
            String owner = classMapping.getFullObfuscatedName();
            out.acceptClass(owner, classMapping.getFullDeobfuscatedName());

            for (MethodMapping methodMapping : classMapping.getMethodMappings()) {
                out.acceptMethod(new IMappingProvider.Member(owner, methodMapping.getObfuscatedName(), methodMapping.getObfuscatedDescriptor()), methodMapping.getDeobfuscatedName());
            }

            for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
                fieldMapping.getType().ifPresent(fieldType -> {
                    out.acceptField(new IMappingProvider.Member(owner, fieldMapping.getObfuscatedName(), fieldType.toString()), fieldMapping.getDeobfuscatedName());
                });
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
