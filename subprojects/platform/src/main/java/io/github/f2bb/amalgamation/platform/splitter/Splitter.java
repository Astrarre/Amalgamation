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

package io.github.f2bb.amalgamation.platform.splitter;

import io.github.f2bb.amalgamation.Platform;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

public interface Splitter {

    Splitter DEFAULT = (in, available) -> false;

    /**
     * Strips the class by filtering out unavailable platforms
     *
     * <blockquote><pre>
     *     {@literal @}Platform({"a"})
     *     {@literal @}Platform({"b"})
     *     public void method() {}
     * </pre></blockquote>
     * <p>
     * Assume the platform <code>b</code> was available in this case. The first predicate (being the
     * {@literal @}{@link Platform} annotation) will not succeed, as <code>a</code> is not
     * available, but the second predicate will be successful, as <code>b</code> is available
     * <br>
     *
     * <blockquote><pre>
     *     {@literal @}Platform({"a", "c"})
     *     {@literal @}Platform({"b", "c"})
     *     public void method() {}
     * </pre></blockquote>
     * <p>
     * Assume the platform <code>b</code> was available in this case. The first predicate (being the
     * {@literal @}{@link Platform} annotation) will not succeed, as BOTH <code>a</code> nor <code>b</code> is not
     * available, as so will the second predicate, as <code>c</code> is not available
     *
     * @param in        The class which will be stripped. This will be mutated
     * @param available The platforms which are available
     * @return Was the class entirely stripped
     */
    boolean strip(ClassNode in, Set<String> available);
}
