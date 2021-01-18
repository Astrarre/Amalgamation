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

import org.objectweb.asm.tree.ClassNode;

import java.util.concurrent.Executor;

/**
 * All methods in this class must be thread safe
 */
public interface MergeContext {

    /**
     * @return The executor to use for merging. Can dramatically increase speed
     */
    Executor getExecutor();

    /**
     * Accepts a merged class
     *
     * @param node The merged class
     */
    void accept(ClassNode node);

    /**
     * Accepts content from a non-class file, or a file which was skipped by {@link #shouldAttemptMerge(PlatformData, String)}
     *
     * @param platform The platform which has this resource
     * @param name     The name of this resource
     * @param bytes    The content of this resource
     */
    void acceptResource(PlatformData platform, String name, byte[] bytes);

    /**
     * Should an attempt be made to merge the class. Otherwise, the class will be treated as a resource
     *
     * @param platform The platform origin
     * @param name     The file name, which includes the <code>.class</code> at the end
     * @return Should the class be merged
     */
    boolean shouldAttemptMerge(PlatformData platform, String name);
}
