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

package io.github.f2bb.amalgamation.source.merger;

import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.Set;

public interface SourceMerger<T extends TypeDeclaration<T>> {

    SourceMerger<?> DEFAULT = allOf(
            SuperclassSourceMerger.INSTANCE
    );

    default boolean isApplicable(TypeDeclaration<?> typeDeclaration) {
        return true;
    }

    void merge(SourceHelper helper, T node, Set<SourceInfo<T>> infos);

    static <T extends TypeDeclaration<T>> SourceMerger<T> allOf(SourceMerger<T>... mergers) {
        return (helper, node, sourceInfos) -> {
            for (SourceMerger<T> merger : mergers) {
                if (merger.isApplicable(node)) {
                    merger.merge(helper, node, sourceInfos);
                }
            }
        };
    }
}
