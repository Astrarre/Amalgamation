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

package io.github.f2bb.amalgamation.gradle.tasks;

import io.github.f2bb.amalgamation.gradle.impl.MappingUtils;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.io.IOException;

public class RemapJar extends Jar {

    private FileCollection classpath;

    private MappingSet mappings;

    @InputFiles
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    @Input
    public MappingSet getMappings() {
        return mappings;
    }

    public void setMappings(MappingSet mappings) {
        this.mappings = mappings;
    }

    @TaskAction
    public void remap() throws IOException {
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(out -> MappingUtils.iterateClasses(mappings, classMapping -> {
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
                }))
                .build();

        FileCollection inputs = getInputs().getFiles();

        for (File file : inputs) {
            remapper.readInputsAsync(file.toPath());
        }

        for (File file : classpath) {
            remapper.readClassPathAsync(file.toPath());
        }

        try (OutputConsumerPath outputConsumerPath = new OutputConsumerPath.Builder(getArchiveFile().get().getAsFile().toPath()).build()) {
            for (File file : inputs) {
                outputConsumerPath.addNonClassFiles(file.toPath());
            }

            remapper.apply(outputConsumerPath);
        } finally {
            remapper.finish();
        }
    }
}
