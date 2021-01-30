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

package io.github.f2bb.amalgamation.gradle.impl.cache;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Cache {

    private final Path basePath;

    public Cache(Path basePath) {
        this.basePath = basePath;
    }

    public Path computeIfAbsent(String extension, Consumer<PrimitiveSink> sink, Populator populator) {
        String hash;
        byte[] log;

        {
            Hasher hasher = Hashing.sha256().newHasher();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            try (PrintStream printStream = new PrintStream(stream)) {
                new Throwable().printStackTrace(printStream);
                sink.accept(new LoggingSink(hasher, printStream));
            }

            hash = hasher.hash().toString();
            log = stream.toByteArray();
        }

        Path path = basePath.resolve(hash);
        Path jar = path.resolve("output" + (extension.isEmpty() ? "" : "." + extension));

        if (!Files.exists(jar)) {
            try {
                Path logPath = path.resolve("log.txt");
                Files.createDirectories(path);
                Files.deleteIfExists(logPath);

                Files.write(logPath, log);
                populator.populate(jar);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        return jar;
    }
}
