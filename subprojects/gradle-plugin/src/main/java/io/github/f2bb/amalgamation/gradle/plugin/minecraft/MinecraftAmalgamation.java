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

package io.github.f2bb.amalgamation.gradle.plugin.minecraft;

import io.github.f2bb.amalgamation.gradle.dependencies.RemappingDependency;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamation;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;

public interface MinecraftAmalgamation extends BaseAmalgamation {
    /**
     * @return a dependency for the obfuscated client jar
     */
    Dependency client(String version);

    /**
     * @return a dependency for the obfuscated server jar (dependencies stripped)
     */
    Dependency server(String version);

    /**
     * @return a list of the remapped dependencies
     */
    Dependency map(Action<RemappingDependency> mappings);
}
