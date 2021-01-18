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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SimpleMergeContext implements MergeContext {

    private final Path root;
    private final Executor executor;

    public SimpleMergeContext(Path root) {
        this(root, Executors.newWorkStealingPool());
    }

    public SimpleMergeContext(Path root, Executor executor) {
        this.root = root;
        this.executor = executor;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public void accept(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        try {
            Path path = root.resolve(node.name + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, writer.toByteArray());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void acceptResource(PlatformData platform, String name, byte[] bytes) {
        try {
            Path path = root.resolve(name);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public boolean shouldAttemptMerge(PlatformData platform, String name) {
        return true;
    }
}
