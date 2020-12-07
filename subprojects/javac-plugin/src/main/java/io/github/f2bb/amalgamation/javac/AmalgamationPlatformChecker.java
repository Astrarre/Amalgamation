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

package io.github.f2bb.amalgamation.javac;

import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AmalgamationPlatformChecker {

    private final Set<String> availablePlatforms;

    public AmalgamationPlatformChecker(Set<String> availablePlatforms) {
        this.availablePlatforms = availablePlatforms;
    }

    public boolean isInvalid(List<JCTree.JCAnnotation> annotations) {
        boolean possiblyInvalid = false;

        for (JCTree.JCAnnotation annotation : annotations) {
            if (!annotation.hasTag(JCTree.Tag.ANNOTATION) && !annotation.hasTag(JCTree.Tag.TYPE_ANNOTATION)) {
                continue;
            }

            if (!annotation.annotationType.hasTag(JCTree.Tag.IDENT)) {
                continue;
            }

            // TODO: Resolve the annotation properly
            if (((JCTree.JCIdent) annotation.annotationType).name.toString().endsWith("Platform")) {
                if (annotation.args.stream()
                        .map(t -> t instanceof JCTree.JCLiteral ? ((JCTree.JCLiteral) t).value : null)
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .allMatch(availablePlatforms::contains)) {
                    return false;
                } else {
                    possiblyInvalid = true;
                }
            }
        }

        return possiblyInvalid;
    }
}
