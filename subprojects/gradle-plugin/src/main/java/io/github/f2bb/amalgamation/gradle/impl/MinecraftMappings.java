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

import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import org.cadixdev.lorenz.MappingSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

class MinecraftMappings {

    final MappingSet officialToIntermediary;
    final MappingSet intermediaryToNamed;
    final MappingSet officialToNamed;

    MinecraftMappings(MappingSet officialToIntermediary, MappingSet intermediaryToNamed, MappingSet officialToNamed) {
        this.officialToIntermediary = officialToIntermediary;
        this.intermediaryToNamed = intermediaryToNamed;
        this.officialToNamed = officialToNamed;
    }

    static void load(Path path, MinecraftMappings mappings) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(path, (ClassLoader) null);
             BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
            TinyTree tree = TinyMappingFactory.load(reader, false);

            new TinyMappingsReader(tree, "official", "intermediary").read(mappings.officialToIntermediary);
            new TinyMappingsReader(tree, "official", "named").read(mappings.officialToNamed);
            new TinyMappingsReader(tree, "intermediary", "named").read(mappings.intermediaryToNamed);
        }
    }
}
