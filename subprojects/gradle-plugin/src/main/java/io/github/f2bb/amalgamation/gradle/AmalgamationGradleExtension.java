/*
 * Amalgamation
 * Copyright (C) 2020 Astrarre
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

package io.github.f2bb.amalgamation.gradle;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.f2bb.amalgamation.platform.merger.PlatformData;
import io.github.f2bb.amalgamation.platform.merger.PlatformMerger;
import io.github.f2bb.amalgamation.platform.merger.SimpleMergeContext;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AmalgamationGradleExtension {

    private final Project project;
    private final List<PlatformSpec> platforms;

    public AmalgamationGradleExtension(Project project) {
        this.project = project;
        this.platforms = new ArrayList<>();
    }

    public void generic(Action<PlatformSpec> action) {
        PlatformSpec platform = new PlatformSpec(project.getDependencies());
        action.execute(platform);
        platforms.add(platform);
    }

    public File createMergedJar() throws IOException {
        Map<PlatformSpec, Set<File>> inputs = platforms.stream()
                .collect(Collectors.toMap(Function.identity(), platform -> project.getConfigurations().detachedConfiguration(platform.dependencies.toArray(new Dependency[0])).getFiles()));
        File outputFile;

        {
            Hasher hasher = Hashing.sha512().newHasher();

            for (Map.Entry<PlatformSpec, Set<File>> entry : inputs.entrySet()) {
                for (String name : entry.getKey().name) {
                    hasher.putUnencodedChars(name);
                }

                for (File file : entry.getValue()) {
                    hasher.putBytes(Files.readAllBytes(file.toPath()));
                }
            }

            String hash = hasher.hash().toString();
            outputFile = project.file(".gradle/amalgamation/" + hash + ".jar");

            if (outputFile.exists()) {
                return outputFile;
            }
        }

        Set<PlatformData> platforms = new HashSet<>();

        for (PlatformSpec platform : this.platforms) {
            Map<String, byte[]> files = new HashMap<>();

            for (File file : inputs.get(platform)) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(), null)) {
                    files.putAll(PlatformData.readFiles(fileSystem.getPath("/")));
                }
            }

            platforms.add(new PlatformData(platform.name, files));
        }

        Files.createDirectories(outputFile.toPath().getParent());
        try (FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:" + outputFile.toURI()), new HashMap<String, Object>() {{
            put("create", "true");
        }})) {
            PlatformMerger.merge(new SimpleMergeContext(fileSystem.getPath("/")), platforms);
        }

        return outputFile;
    }

    public FileCollection getClasspath(Collection<String> platforms) {
        return project.files(project.provider(() -> {
            ConfigurableFileCollection files = project.files();

            for (PlatformSpec platform : this.platforms) {
                if (platforms.containsAll(platform.name)) {
                    files.from(project.getConfigurations().detachedConfiguration(platform.dependencies.toArray(new Dependency[0])).getAsFileTree());
                }
            }

            return files;
        }));
    }

    public FileCollection transform(Object input, Collection<String> platforms) {
        return project.files(project.provider(() -> {
            File root = project.getBuildDir().toPath().resolve("tmp").resolve("amalgamation").resolve(UUID.randomUUID().toString()).toFile();
            ConfigurableFileCollection files = project.files();

            FileTree tree = project.files(input).getAsFileTree();

            tree.visit(details -> {
                File target = details.getRelativePath().getFile(root);

                if (details.getName().endsWith(".class")) {
                    // TODO: Transform
                    details.copyTo(target);
                    System.out.println("Transform " + details.getRelativePath());
                } else {
                    details.copyTo(target);
                }
            });

            return files;
        }));
    }
}
