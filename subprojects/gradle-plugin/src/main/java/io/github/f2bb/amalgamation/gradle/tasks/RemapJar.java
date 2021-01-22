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
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RemapJar extends Jar {

    @Input
    private FileCollection classpath;

    @Input
    private MappingSet mappings;

    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    public void mappings(MappingSet mappings) {
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
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (File file : inputs) {
            futures.add(remapper.readInputsAsync(file.toPath()));
        }

        for (File file : classpath) {
            futures.add(remapper.readClassPathAsync(file.toPath()));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

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
