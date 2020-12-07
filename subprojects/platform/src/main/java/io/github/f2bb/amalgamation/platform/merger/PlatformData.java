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

package io.github.f2bb.amalgamation.platform.merger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Information about the platform, such as its name(s) and files
 */
public class PlatformData {

    final Set<String> name;
    final Map<String, byte[]> files;

    public PlatformData(Set<String> name, Map<String, byte[]> files) {
        this.name = name;
        this.files = files;
    }

    @Override
    public String toString() {
        return "PlatformData{" +
                "name=" + name +
                '}';
    }

    public static PlatformData createFromArchive(Path archive, String... name) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(archive, null)) {
            return create(fileSystem.getPath("/"), name);
        }
    }

    public static PlatformData create(Path root, String... name) throws IOException {
        Map<String, byte[]> files = new HashMap<>();

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.put(root.relativize(file).toString(), Files.readAllBytes(file));
                return FileVisitResult.CONTINUE;
            }
        });

        return new PlatformData(new HashSet<>(Arrays.asList(name)), files);
    }
}
