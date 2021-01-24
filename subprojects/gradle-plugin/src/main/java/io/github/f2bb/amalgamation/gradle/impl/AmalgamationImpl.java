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

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.gradle.base.GenericPlatformSpec;
import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.merger.PlatformMerger;
import io.github.f2bb.amalgamation.platform.merger.SimpleMergeContext;
import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AmalgamationImpl {

    private static final String GROUP = "amalgamation-merged";
    private static final String VERSION = "1.0.0";
    private static final String MERGED_POM = "" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>" + GROUP + "</groupId>\n" +
            "    <artifactId>{hash}</artifactId>\n" +
            "    <version>" + VERSION + "</version>\n" +
            "</project>\n";

    public static Dependency createDependencyFromMatrix(Project project, Dependency mappings, Set<Forge> forgeSpecs, Set<Fabric> fabricSpecs, Set<GenericPlatformSpec> genericSpecs) throws IOException {
        if (forgeSpecs.isEmpty() && fabricSpecs.isEmpty() && genericSpecs.isEmpty()) {
            throw new IllegalStateException("No dependencies are present");
        }

        String hash = hash(project, mappings, forgeSpecs, fabricSpecs, genericSpecs);
        Path file = getJarCoordinates(project, hash);

        if (!Files.exists(file)) {
            processInternal(file, project, mappings, forgeSpecs, fabricSpecs, genericSpecs);
        }

        return project.getDependencies().create(GROUP + ":" + hash + ":" + VERSION);
    }

    private static void processInternal(Path outputFile, Project project, Dependency mappingsDependency, Set<Forge> forgeSpecs, Set<Fabric> fabricSpecs, Set<GenericPlatformSpec> genericSpecs) throws IOException {
        Set<PlatformData> platforms = new HashSet<>();

        for (GenericPlatformSpec spec : genericSpecs) {
            Map<String, byte[]> files = new HashMap<>();

            for (Dependency dependency : spec.getDependencies()) {
                for (File file : resolve(project, dependency)) {
                    try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null)) {
                        PlatformData.readFiles(files, fileSystem.getPath("/"));
                    }
                }
            }

            platforms.add(new PlatformData(spec.getNames(), files));
        }

        MappingSet mappings;

        if (mappingsDependency != null) {
            mappings = loadMappings(resolve(project, mappingsDependency));
        } else {
            mappings = null;
        }

        for (Forge spec : forgeSpecs) {
            Map<String, byte[]> files = new HashMap<>();

            for (Path path : spec.getFiles(mappings)) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(path, (ClassLoader) null)) {
                    PlatformData.readFiles(files, fileSystem.getPath("/"));
                }
            }

            platforms.add(new PlatformData(spec.forge.getNames(), files));
        }

        for (Fabric spec : fabricSpecs) {
            Map<String, byte[]> files = new HashMap<>();

            for (Path path : spec.getFiles(mappings)) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(path, (ClassLoader) null)) {
                    PlatformData.readFiles(files, fileSystem.getPath("/"));
                }
            }

            platforms.add(new PlatformData(spec.fabric.getNames(), files));
        }

        Files.createDirectories(outputFile.getParent());
        try (FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:" + outputFile.toUri()), new HashMap<String, Object>() {{
            put("create", "true");
        }})) {
            PlatformMerger.merge(new SimpleMergeContext(fileSystem.getPath("/")), platforms);
        } catch (Throwable throwable) {
            Files.deleteIfExists(outputFile);
            throw throwable;
        }
    }

    private static MappingSet loadMappings(Set<File> files) throws IOException {
        MappingSet mappings = MappingSet.create();

        for (File file : files) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null);
                 BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
                new TinyMappingsReader(TinyMappingFactory.loadWithDetection(reader), "intermediary", "named").read(mappings);
            }
        }

        return mappings;
    }

    private static String hash(Project project, Dependency mappings, Set<Forge> forgeSpecs, Set<Fabric> fabricSpecs, Set<GenericPlatformSpec> genericSpecs) throws IOException {
        Hasher hasher = Hashing.sha256().newHasher();

        if (mappings != null) {
            put(hasher, resolve(project, mappings));
        }

        for (Forge spec : forgeSpecs) {
            hasher.putUnencodedChars(spec.minecraftVersion);

            put(hasher, resolve(project, spec.dependency));

            for (Dependency dependency : spec.forge.getDependencies()) {
                put(hasher, resolve(project, dependency));
            }

            for (Dependency dependency : spec.forge.getRemap()) {
                put(hasher, resolve(project, dependency));
            }
        }

        for (Fabric spec : fabricSpecs) {
            hasher.putUnencodedChars(spec.minecraftVersion);

            for (Dependency dependency : spec.fabric.getDependencies()) {
                put(hasher, resolve(project, dependency));
            }

            for (Dependency dependency : spec.fabric.getRemap()) {
                put(hasher, resolve(project, dependency));
            }
        }

        for (GenericPlatformSpec spec : genericSpecs) {
            for (Dependency dependency : spec.getDependencies()) {
                put(hasher, resolve(project, dependency));
            }
        }

        return hasher.hash().toString();
    }

    private static void put(Hasher hasher, Set<File> files) throws IOException {
        for (File file : files) {
            hasher.putBytes(Files.readAllBytes(file.toPath()));
        }
    }

    static Set<File> resolve(Project project, Dependency dependency) {
        return project.getConfigurations().detachedConfiguration(dependency).resolve();
    }

    private static Path getJarCoordinates(Project project, String hash) throws IOException {
        Path root = project.getRootDir().toPath().resolve(".gradle").resolve("amalgamation");
        Path folder = Files.createDirectories(root.resolve(GROUP).resolve(hash).resolve(VERSION));
        Path jar = folder.resolve(hash + "-" + VERSION + ".jar");
        Path pom = folder.resolve(hash + "-" + VERSION + ".pom");

        blessed:
        {
            for (ArtifactRepository repository : project.getRepositories()) {
                if (repository instanceof MavenArtifactRepository && ((MavenArtifactRepository) repository).getUrl().equals(root.toUri())) {
                    break blessed;
                }
            }

            project.getRepositories().maven(repository -> {
                repository.setName("Amalgamation");
                repository.setUrl(root.toUri());
            });
        }


        if (!Files.exists(pom)) {
            Files.write(pom, MERGED_POM.replace("{hash}", hash).getBytes(StandardCharsets.UTF_8));
        }

        return jar;
    }
}
