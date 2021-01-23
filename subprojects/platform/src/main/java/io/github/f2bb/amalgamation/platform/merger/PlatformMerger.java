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

import io.github.f2bb.amalgamation.Platform;
import io.github.f2bb.amalgamation.platform.merger.impl.Merger;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PlatformMerger {

	private static final String PLATFORM_DESCRIPTOR = Type.getDescriptor(Platform.class);

	public static void merge(MergeContext mergeContext, Set<PlatformData> platforms) {
		Map<String, Set<PlatformData>> mergeClasses = new HashMap<>();

		for (PlatformData platform : platforms) {
			platform.files.forEach((name, bytes) -> {
				if (name.endsWith(".class") && mergeContext.shouldAttemptMerge(platform, name)) {
					mergeClasses.computeIfAbsent(name, $ -> new HashSet<>()).add(platform);
				} else {
					mergeContext.acceptResource(platform, name, bytes);
				}
			});
		}

		mergeClasses(mergeContext, mergeClasses, platforms);
	}

	private static void mergeClasses(MergeContext mergeContext,
									 Map<String, Set<PlatformData>> classes,
									 Set<PlatformData> availablePlatforms) {
		// TODO: I'm going to say the n-way
		Set<CompletableFuture<?>> futures = new HashSet<>();

		classes.forEach((file, platforms) -> futures.add(CompletableFuture.runAsync(() -> {
			if (platforms.size() == 1) {
				// This class is only present on one platform
				// This is really a specialisation and optimisation of what would be the n-way algorithm

				PlatformData platform = platforms.iterator().next();
				ClassNode node = read(platform.files.get(file));

				if (availablePlatforms.size() > 1) {
					// There are multiple platforms
					AnnotationNode annotation = new AnnotationNode(PLATFORM_DESCRIPTOR);
					mark(annotation.visitArray("value"), platform);

					if (node.invisibleAnnotations == null) {
						node.invisibleAnnotations = new ArrayList<>();
					}

					node.invisibleAnnotations.add(annotation);
				} else {
					// This is the only platform, just copy
				}

				mergeContext.accept(node);
			} else {
				List<ClassInfo> infos = new ArrayList<>();

				for (PlatformData platform : platforms) {
					infos.add(new ClassInfo(read(platform.files.get(file)), platform.name.toArray(new String[0])));
				}

				ClassNode merged = new ClassNode();
				Merger.MERGER.merge(merged, infos);
				mergeContext.accept(merged);
			}
		}, mergeContext.getExecutor())));

		try {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
		} catch (InterruptedException | ExecutionException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static ClassNode read(byte[] bytes) {
		ClassNode node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		return node;
	}

	private static void mark(AnnotationVisitor value, PlatformData platform) {
		for (String s : platform.name) {
			value.visit(null, s);
		}
	}
}
