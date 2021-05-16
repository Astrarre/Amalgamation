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

package io.github.astrarre.merger.context;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DefaultMergeContext implements MergeContext {
	private final Iterable<Path> mergedJarRoots;
	private final Executor executor;
	private final Map<String, List<String>> idMap;

	public DefaultMergeContext(Iterable<Path> mergedJarRoots, Map<String, List<String>> map) {
		this(mergedJarRoots, Executors.newWorkStealingPool(), map);
	}

	public DefaultMergeContext(Iterable<Path> mergedJarRoots, Executor executor, Map<String, List<String>> map) {
		this.mergedJarRoots = mergedJarRoots;
		this.executor = executor;
		this.idMap = map;
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
	public void acceptResource(PlatformData platform, String name, Path input) {
		try {
			for (Path root : this.mergedJarRoots) {
				Path path = root.resolve(name);
				if (!Files.exists(path)) {
					Files.createDirectories(path.getParent());
					Files.copy(input, path);
				} else {
					// todo warn dupes
				}
			}
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void close() throws Exception {

	}

	@Override
	public boolean shouldAttemptMerge(PlatformData platform, String name) {
		return true;
	}

	@Override
	public Map<String, List<String>> idMap() {
		return this.idMap;
	}
}
