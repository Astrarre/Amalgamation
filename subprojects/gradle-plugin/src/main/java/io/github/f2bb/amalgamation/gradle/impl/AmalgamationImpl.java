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
import io.github.f2bb.amalgamation.gradle.minecraft.GenericPlatformSpec;
import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactType;
import net.minecraftforge.artifactural.base.artifact.SimpleArtifactIdentifier;
import net.minecraftforge.artifactural.base.artifact.StreamableArtifact;
import net.minecraftforge.artifactural.gradle.GradleRepositoryAdapter;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

public class AmalgamationImpl {

    private static final String MERGED_POM =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>amalgamation-merged</groupId>\n" +
            "    <artifactId>{hash}</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "</project>\n";

    public static Dependency createDependencyFromMatrix(Project project, Dependency mappings, Set<Forge> forgeSpecs, Set<Fabric> fabricSpecs, Set<GenericPlatformSpec> genericSpecs) throws IOException {
        String hash = hash(project, mappings, forgeSpecs, fabricSpecs, genericSpecs);
        SimpleArtifactIdentifier jarIdentifier = new SimpleArtifactIdentifier("amalgamation-merged", hash, "1.0.0", null, "jar");
        SimpleArtifactIdentifier pomIdentifier = new SimpleArtifactIdentifier("amalgamation-merged", hash, "1.0.0", null, "pom");

        Map<String, Artifact> adapter = getAdapter(project);
        adapter.put(jarIdentifier.toString(), StreamableArtifact.ofStreamable(jarIdentifier, ArtifactType.BINARY, () -> processInternal(project, mappings, forgeSpecs, fabricSpecs, genericSpecs)));
        adapter.put(pomIdentifier.toString(), StreamableArtifact.ofBytes(pomIdentifier, ArtifactType.OTHER, MERGED_POM.replace("{hash}", hash).getBytes(StandardCharsets.UTF_8)));

        return project.getDependencies().create(jarIdentifier.toString());
    }

    private static InputStream processInternal(Project project, Dependency mappings, Set<Forge> forgeSpecs, Set<Fabric> fabricSpecs, Set<GenericPlatformSpec> genericSpecs) {
        // TODO: Make it actually work
        return null;
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

    private static Set<File> resolve(Project project, Dependency dependency) {
        return project.getConfigurations().detachedConfiguration(dependency).resolve();
    }

    private static Map<String, Artifact> getAdapter(Project project) {
        for (ArtifactRepository repository : project.getRepositories()) {
            if (repository instanceof GradleRepositoryAdapter) {
                try {
                    Field field = GradleRepositoryAdapter.class.getDeclaredField("repository");
                    field.setAccessible(true);
                    Object o = field.get(repository);

                    if (o instanceof MapRepository) {
                        return ((MapRepository) o).map;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }

        MapRepository repository = new MapRepository();
        GradleRepositoryAdapter.add(project.getRepositories(), "amalgamation", new File(project.getRootDir(), ".gradle/amalgamation"), repository);
        return repository.map;
    }
}
