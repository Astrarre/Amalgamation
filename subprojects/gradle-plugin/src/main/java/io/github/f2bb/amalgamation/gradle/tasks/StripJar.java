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

package io.github.f2bb.amalgamation.gradle.tasks;

import io.github.f2bb.amalgamation.platform.splitter.Splitter;
import org.gradle.api.Action;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.tasks.Input;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StripJar extends Jar {

    @Input
    private final Set<String> platforms = new HashSet<>();

    public StripJar() {
        getMainSpec().appendCachingSafeCopyAction(new MyAction());
    }

    public Set<String> getPlatforms() {
        return platforms;
    }

    public void platform(Object platform) {
        platforms.add(String.valueOf(platform));
    }

    private class MyAction implements Action<FileCopyDetails> {

        @Override
        public void execute(@NotNull FileCopyDetails fileCopyDetails) {
            ClassNode node = new ClassNode();

            try {
                new ClassReader(fileCopyDetails.open()).accept(node, 0);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            if (Splitter.DEFAULT.strip(node, platforms)) {
                fileCopyDetails.exclude();
            } else {
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);

                Map<String, Object> properties = new HashMap<>();
                properties.put("data", new InputStreamReader(new ByteArrayInputStream(writer.toByteArray())));
                fileCopyDetails.filter(properties, ByteReading.class);
            }
        }
    }
}
