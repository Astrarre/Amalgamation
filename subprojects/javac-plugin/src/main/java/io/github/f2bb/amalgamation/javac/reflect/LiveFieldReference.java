/*
 * Amalgamation
 * Copyright (C) 2020 IridisMC
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

import java.lang.reflect.Field;

public class LiveFieldReference<T> {

    private final Object bound;
    private final Field field;

    public LiveFieldReference(Object bound, Field field) {
        this.bound = bound;
        this.field = field;
    }

    public T get() {
        try {
            return (T) field.get(bound);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(T t) {
        try {
            field.set(bound, t);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}