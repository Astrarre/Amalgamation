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

package io.github.f2bb.amalgamation.platform.merger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AbstractMergeContext implements MergeContext {
	private final Iterable<Path> mergedJarRoots;
	private final Executor executor;

	public AbstractMergeContext(Iterable<Path> mergedJarRoots) {
		this(mergedJarRoots, Executors.newWorkStealingPool());
	}

	public AbstractMergeContext(Iterable<Path> mergedJarRoots, Executor executor) {
		this.mergedJarRoots = mergedJarRoots;
		this.executor = executor;
	}

	@Override
	public Executor getExecutor() {
		return this.executor;
	}

	@Override
	public void accept(ClassNode node) {
		ClassWriter writer = new ClassWriter(0);
		node.accept(writer);

		try {
			for (Path mergedJarRoot : this.mergedJarRoots) {
				Path path = mergedJarRoot.resolve(node.name + ".class");
				if (!Files.exists(path)) {
					Files.createDirectories(path.getParent());
					Files.write(path, writer.toByteArray());
				} else {
					System.err.println(path + " is duplicated!");
				}
			}

		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void acceptResource(PlatformData platform, String name, byte[] bytes) {
		try {
			for (Path root : this.mergedJarRoots) {
				Path path = root.resolve(name);
				if (!Files.exists(path)) {
					Files.createDirectories(path.getParent());
					Files.write(path, bytes);
				} else {
					System.err.println(path + " is duplicated!");
				}
			}
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean shouldAttemptMerge(PlatformData platform, String name) {
		return true;
	}
}
