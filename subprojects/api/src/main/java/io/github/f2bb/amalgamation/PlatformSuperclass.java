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

package io.github.f2bb.amalgamation;

import java.lang.annotation.*;

/**
 * Tells the Java compiler that the specified superclass should be use when the platform constraints are met.
 * Also serves as a marker so developers know on which platforms a superclass is used instead of another
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(PlatformSuperclasses.class)
public @interface PlatformSuperclass {

    Class<?> value();

    Platform platform();
}
