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
import org.gradle.api.Project;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class Cache {

    private final Path basePath;

    public Cache(Path basePath) {
        this.basePath = basePath;
    }

    public Path computeIfAbsent(String output, UnsafeConsumer<PrimitiveSink> sink, UnsafeConsumer<Path> populator) {
        String hash;
        byte[] log;

        {
            Hasher hasher = Hashing.sha256().newHasher();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            try (PrintStream printStream = new PrintStream(stream)) {
                new Throwable().printStackTrace(printStream);

                try {
                    sink.accept(new LoggingSink(hasher, printStream));
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }

            hash = hasher.hash().toString();
            log = stream.toByteArray();
        }

        Path path = basePath.resolve(hash);
        Path out = path.resolve(output);
        Path ok = path.resolve("ok");

        // OK marker
        if (!Files.exists(ok)) {
            try {
                Path logPath = path.resolve("log.txt");
                Files.createDirectories(path);
                Files.deleteIfExists(logPath);

                Files.write(logPath, log);
                populator.accept(out);
                Files.write(ok, new byte[0]);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        return out;
    }

    public Path download(String output, URL url) {
        return computeIfAbsent(output, sink -> {
            sink.putUnencodedChars("download");
            sink.putUnencodedChars(url.toString());
        }, path -> {
            try (InputStream inputStream = url.openStream()) {
                Files.copy(inputStream, path);
            }
        });
    }

    public static Cache of(Project project) {
        return new Cache(project.getRootDir().toPath().resolve(".gradle").resolve("amalgamation").resolve("cache"));
    }
}
