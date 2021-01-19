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

package io.github.f2bb.amalgamation.javac.reflect;

import com.sun.tools.javac.util.List;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class JavacList {

    public static <T> void transform(LiveFieldReference<List<T>> reference, UnaryOperator<java.util.List<T>> transformer) {
        reference.set(List.from(transformer.apply(new ArrayList<>(reference.get()))));
    }

    public static <T> void removeIf(LiveFieldReference<List<T>> reference, Predicate<T> predicate) {
        transform(reference, l -> {
            l.removeIf(predicate);
            return l;
        });
    }
}
