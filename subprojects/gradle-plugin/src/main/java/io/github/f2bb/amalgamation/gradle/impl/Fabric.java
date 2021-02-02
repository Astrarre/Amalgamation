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
import com.google.common.collect.Sets;
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
import org.gradle.internal.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    public ClientServer getFiles(MappingSet mappings, Set<File> mappingsFiles) throws IOException {
        Cache cache = Cache.globalCache(project);
        Path workingDirectory = Files.createDirectories(project.getBuildDir().toPath().resolve("amalgamation"));

        // Step 1 - Download Minecraft
        Path clientJar;
        Path serverJar;
        List<String> libraries = new ArrayList<>();

        project.getLogger().debug("getting version manifest . . .");
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

            project.getLogger().debug("getting client . . .");
            clientJar = cache.download(this.minecraftVersion + "-client.jar", new URL(client));

            project.getLogger().debug("getting server . . .");
            Path unstrippedServerJar = cache.download(this.minecraftVersion + "-server.jar", new URL(server));

            project.getLogger().debug("getting server without libraries . . .");
            serverJar = cache.computeIfAbsent(this.minecraftVersion + "-stripped-server.jar", sink -> {
                sink.putUnencodedChars("Strip libraries");
                sink.putLong(Files.getLastModifiedTime(unstrippedServerJar).toMillis());
            }, output -> {
                Files.copy(unstrippedServerJar, output);
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
        Pair<MappingSet, Collection<File>> officialToIntermediary = officialToIntermediary();
        // todo what if intermediary re-releases or smth, this needs better caching
        // todo use the same TinyRemapper instance
        project.getLogger().debug("getting intermediary client . . .");
        Path intermediaryClientJar = remap(cache, this.minecraftVersion + "-intermediary-client.jar", officialToIntermediary.left, clientJar, officialToIntermediary.right);
        project.getLogger().debug("getting intermediary server . . .");
        Path intermediaryServerJar = remap(cache, this.minecraftVersion + "-intermediary-server.jar", officialToIntermediary.left, serverJar, officialToIntermediary.right);

        // Step 3 - Collect inputs
        Configuration remap = fabric.getRemap().copy();

        for (String library : libraries) {
            remap.getDependencies().add(project.getDependencies().create(library));
        }

        project.getRepositories().maven(repository -> {
            repository.setName("Minecraft Libraries");
            repository.setUrl("https://libraries.minecraft.net/");
        });

        // Step 4 - Remap everything to named
        Set<Path> temporary = new HashSet<>();
        Set<Path> toMergeClient = something(temporary, mappings, intermediaryClientJar, remap, workingDirectory, mappingsFiles);
        Set<Path> toMergeServer = something(temporary, mappings, intermediaryServerJar, Collections.emptySet(), workingDirectory, mappingsFiles);

        for (File file : fabric.getDependencies()) {
            toMergeClient.add(file.toPath());
            toMergeServer.add(file.toPath());
        }

        return new ClientServer(temporary, toMergeClient, toMergeServer);
    }


    /**
     * @param with the 'main' jar to remap
     * @param remap everything else to remap
     */
    private Set<Path> something(Set<Path> temporary, MappingSet mappings, Path with, Iterable<File> remap, Path workingDirectory, Set<File> mappingsFiles) throws IOException {
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(MappingUtils.createMappingProvider(mappings))
                .build();

        Set<Path> yes = new HashSet<>();
        Map<Path, InputTag> tags = new HashMap<>();

        for (File file : Iterables.concat(remap, Collections.singleton(with.toFile()))) {
            InputTag tag = remapper.createInputTag();
            tags.put(file.toPath(), tag);
            remapper.readInputsAsync(tag, file.toPath());
        }

        for (File file : fabric.getDependencies()) {
            remapper.readClassPathAsync(file.toPath());
        }

        for (Map.Entry<Path, InputTag> entry : tags.entrySet()) {
            InputTag tag = entry.getValue();

            Path out = Files.createTempFile(workingDirectory, "mapped", ".jar");
            Files.delete(out);
            yes.add(out);
            temporary.add(out);

            try (OutputConsumerPath output = new OutputConsumerPath.Builder(out).build()) {
                output.addNonClassFiles(entry.getKey(), NonClassCopyMode.FIX_META_INF, remapper);
                remapper.apply(output, tag);
            }
        }

        remapper.finish();
        return yes;
    }

    private Path remap(Cache cache, String output, MappingSet officialToIntermediary, Path jar, Collection<File> files) {
        return cache.computeIfAbsent(output, sink -> {
            sink.putUnencodedChars("Remap official to intermediary");
            sink.putLong(Files.getLastModifiedTime(jar).toMillis());
            for (File file : files) {
                sink.putLong(file.lastModified());
                sink.putString(file.getAbsolutePath(), StandardCharsets.UTF_8);
            }
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

    private Pair<MappingSet, Collection<File>> officialToIntermediary() throws IOException {
        Set<File> files = AmalgamationImpl.resolve(project, project.getDependencies().create("net.fabricmc:intermediary:" + minecraftVersion + ":v2"));
        File file = Iterables.getOnlyElement(files);
        try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(), (ClassLoader) null);
             BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
            return Pair.of(new TinyMappingsReader(TinyMappingFactory.loadWithDetection(reader), "official", "intermediary").read(), files);
        }
    }
}
