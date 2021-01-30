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

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftPlatformSpec;
import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

class Fabric {

    private static final Gson GSON = new Gson();

    private final Project project;
    final String minecraftVersion;
    final MinecraftPlatformSpec fabric;

    public Fabric(Project project, String minecraftVersion, MinecraftPlatformSpec fabric) {
        this.project = project;
        this.minecraftVersion = minecraftVersion;
        this.fabric = fabric;
    }

    public ClasspathClientServer getFiles(MappingSet mappings) throws IOException {
        Cache cache = Cache.of(project);
        Path workingDirectory = Files.createDirectories(project.getBuildDir().toPath().resolve("amalgamation"));

        // Step 1 - Download Minecraft
        Path clientJar;
        Path serverJar;
        List<String> libraries = new ArrayList<>();

        try (Reader globalManifestReader = Files.newBufferedReader(cache.download("version_manifest.json", new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")))) {
            String manifestUrl = null;

            for (JsonElement element : GSON.fromJson(globalManifestReader, JsonObject.class).getAsJsonArray("versions")) {
                JsonObject object = element.getAsJsonObject();
                if (object.get("id").getAsString().equals(minecraftVersion)) {
                    manifestUrl = object.get("url").getAsString();
                    break;
                }
            }

            if (manifestUrl == null) {
                throw new IllegalStateException("Version " + minecraftVersion + " was not found");
            }

            String client;
            String server;

            try (Reader versionManifestReader = Files.newBufferedReader(cache.download("manifest.json", new URL(manifestUrl)))) {
                JsonObject versionManifest = GSON.fromJson(versionManifestReader, JsonObject.class);
                JsonObject downloads = versionManifest.getAsJsonObject("downloads");

                client = downloads.getAsJsonObject("client").get("url").getAsString();
                server = downloads.getAsJsonObject("server").get("url").getAsString();

                for (JsonElement element : versionManifest.getAsJsonArray("libraries")) {
                    libraries.add(element.getAsJsonObject().get("name").getAsString());
                }
            }

            if (client == null) {
                throw new IllegalStateException("Client download for " + minecraftVersion + " was not found");
            }

            if (server == null) {
                throw new IllegalStateException("Client download for " + minecraftVersion + " was not found");
            }

            clientJar = cache.download("client.jar", new URL(client));

            byte[] originalServerJar = Files.readAllBytes(cache.download("server.jar", new URL(server)));

            serverJar = cache.computeIfAbsent("server.jar", sink -> {
                sink.putUnencodedChars("Strip libraries");
                sink.putBytes(originalServerJar);
            }, output -> {
                Files.write(output, originalServerJar);

                try (FileSystem fileSystem = FileSystems.newFileSystem(output, (ClassLoader) null)) {
                    Path root = fileSystem.getPath("/");

                    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String name = root.relativize(file).toString();

                            if (!name.startsWith("net/minecraft/") && name.contains("/")) {
                                Files.delete(file);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            });
        }

        // Step 2 - Remap to intermediary
        MappingSet officialToIntermediary = officialToIntermediary();
        Path intermediaryClientJar = remap(cache, "client.jar", officialToIntermediary, clientJar);
        Path intermediaryServerJar = remap(cache, "server.jar", officialToIntermediary, serverJar);

        // Step 3 - Collect classpath
        Set<File> classpath;

        {
            Configuration dependencies = fabric.getDependencies().copy();

            for (String library : libraries) {
                Dependency dependency = project.getDependencies().create(library);
                dependencies.getDependencies().add(dependency);

                // Also add this Minecraft library to the project classpath
                project.getDependencies().add("compileClasspath", dependency);
            }

            project.getRepositories().maven(repository -> {
                repository.setName("Minecraft Libraries");
                repository.setUrl("https://libraries.minecraft.net/");
            });

            classpath = dependencies.resolve();
        }

        // Step 4 - Remap everything to named
        List<Path> toMerge = new ArrayList<>();
        Path mappedClient = null;
        Path mappedServer = null;

        for (File file : fabric.getDependencies().resolve()) {
            toMerge.add(file.toPath());
        }

        {
            TinyRemapper remapper = TinyRemapper.newRemapper()
                    .withMappings(MappingUtils.createMappingProvider(mappings))
                    .build();
            Map<Path, InputTag> tags = new HashMap<>();
            InputTag clientTag = remapper.createInputTag();
            InputTag serverTag = remapper.createInputTag();

            {
                tags.put(intermediaryClientJar, clientTag);
                remapper.readInputsAsync(clientTag, intermediaryClientJar);
            }

            {
                tags.put(intermediaryServerJar, serverTag);
                remapper.readInputsAsync(serverTag, intermediaryServerJar);
            }

            for (File file : fabric.getRemap().resolve()) {
                InputTag tag = remapper.createInputTag();
                tags.put(file.toPath(), tag);
                remapper.readInputsAsync(tag, file.toPath());
            }

            for (File file : classpath) {
                remapper.readClassPathAsync(file.toPath());
            }

            for (Map.Entry<Path, InputTag> entry : tags.entrySet()) {
                InputTag tag = entry.getValue();

                Path out = Files.createTempFile(workingDirectory, "mapped", ".jar");

                if (tag == clientTag) {
                    mappedClient = out;
                } else if (tag == serverTag) {
                    mappedServer = out;
                } else {
                    toMerge.add(out);
                }

                Files.delete(out);

                try (OutputConsumerPath output = new OutputConsumerPath.Builder(out).build()) {
                    output.addNonClassFiles(entry.getKey(), NonClassCopyMode.FIX_META_INF, remapper);
                    remapper.apply(output, tag);
                }
            }

            remapper.finish();
        }

        return new ClasspathClientServer(toMerge, mappedClient, mappedServer);
    }

    private Path remap(Cache cache, String output, MappingSet officialToIntermediary, Path jar) {
        return cache.computeIfAbsent(output, sink -> {
            sink.putUnencodedChars("Remap official to intermediary");
            sink.putBytes(Files.readAllBytes(jar));
        }, path -> {
            TinyRemapper remapper = TinyRemapper.newRemapper()
                    .withMappings(MappingUtils.createMappingProvider(officialToIntermediary))
                    .build();

            remapper.readInputsAsync(jar);

            try (OutputConsumerPath out = new OutputConsumerPath.Builder(path).build()) {
                out.addNonClassFiles(jar, NonClassCopyMode.FIX_META_INF, remapper);
                remapper.apply(out);
            }

            remapper.finish();
        });
    }

    private MappingSet officialToIntermediary() throws IOException {
        File file = Iterables.getOnlyElement(AmalgamationImpl.resolve(project, project.getDependencies().create("net.fabricmc:intermediary:" + minecraftVersion + ":v2")));

        try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null);
             BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
            return new TinyMappingsReader(TinyMappingFactory.loadWithDetection(reader), "official", "intermediary").read();
        }
    }
}
