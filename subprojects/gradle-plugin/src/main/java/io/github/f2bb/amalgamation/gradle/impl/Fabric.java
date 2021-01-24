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
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftPlatformSpec;
import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
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
        Path workingDirectory = Files.createDirectories(project.getBuildDir().toPath().resolve("amalgamation"));

        // Step 1 - Download Minecraft
        Path clientJar = Files.createTempFile(workingDirectory, "minecraft-client", ".jar");
        Path serverJar = Files.createTempFile(workingDirectory, "minecraft-server", ".jar");
        Path intermediaryClientJar = Files.createTempFile(workingDirectory, "minecraft-intermediary-client", ".jar");
        Path intermediaryServerJar = Files.createTempFile(workingDirectory, "minecraft-intermediary-server", ".jar");
        List<String> libraries = new ArrayList<>();

        Files.delete(clientJar);
        Files.delete(serverJar);
        Files.delete(intermediaryClientJar);
        Files.delete(intermediaryServerJar);

        try (InputStream _a = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream();
             Reader globalManifestReader = new InputStreamReader(_a, StandardCharsets.UTF_8)) {
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

            try (InputStream _b = new URL(manifestUrl).openStream();
                 Reader versionManifestReader = new InputStreamReader(_b, StandardCharsets.UTF_8)) {
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

            try (InputStream inputStream = new URL(client).openStream()) {
                Files.copy(inputStream, clientJar);
            }

            try (InputStream inputStream = new URL(server).openStream()) {
                Files.copy(inputStream, serverJar);
            }
        }

        // Step 2 - Strip embedded libraries inside the server jar
        try (FileSystem fileSystem = FileSystems.newFileSystem(serverJar, (ClassLoader) null)) {
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

        // Step 3 - Remap to intermediary
        {
            TinyRemapper remapper = TinyRemapper.newRemapper()
                    .withMappings(MappingUtils.createMappingProvider(officialToIntermediary()))
                    .build();

            InputTag clientTag = remapper.createInputTag();
            InputTag serverTag = remapper.createInputTag();

            remapper.readInputsAsync(clientTag, clientJar);
            remapper.readInputsAsync(serverTag, serverJar);

            try (OutputConsumerPath output = new OutputConsumerPath.Builder(intermediaryClientJar).build()) {
                output.addNonClassFiles(clientJar, NonClassCopyMode.FIX_META_INF, remapper);
                remapper.apply(output, clientTag);
            }

            try (OutputConsumerPath output = new OutputConsumerPath.Builder(intermediaryServerJar).build()) {
                output.addNonClassFiles(serverJar, NonClassCopyMode.FIX_META_INF, remapper);
                remapper.apply(output, serverTag);
            }

            remapper.finish();
        }

        // Step 4 - Collect classpath
        Set<File> classpath;

        {
            List<Dependency> dependencies = new ArrayList<>(fabric.getDependencies());

            for (String library : libraries) {
                Dependency dependency = project.getDependencies().create(library);
                dependencies.add(dependency);

                // Also add this Minecraft library to the project classpath
                project.getDependencies().add("compileClasspath", dependency);
            }

            project.getRepositories().maven(repository -> {
                repository.setName("Minecraft Libraries");
                repository.setUrl("https://libraries.minecraft.net/");
            });

            classpath = project.getConfigurations().detachedConfiguration(dependencies.toArray(new Dependency[0])).getFiles();
        }

        // Step 5 - Remap everything to named
        List<Path> toMerge = new ArrayList<>();
        Path mappedClient = null;
        Path mappedServer = null;

        for (File file : AmalgamationImpl.resolve(project, fabric.getDependencies())) {
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

            for (File file : AmalgamationImpl.resolve(project, fabric.getRemap())) {
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

    private MappingSet officialToIntermediary() throws IOException {
        File file = Iterables.getOnlyElement(AmalgamationImpl.resolve(project, project.getDependencies().create("net.fabricmc:intermediary:" + minecraftVersion + ":v2")));

        try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null);
             BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
            return new TinyMappingsReader(TinyMappingFactory.loadWithDetection(reader), "official", "intermediary").read();
        }
    }
}
