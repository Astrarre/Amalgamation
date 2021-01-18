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

package io.github.f2bb.amalgamation.gradle;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.*;

public class PlatformSpec {

    private final DependencyHandler handler;

    final Set<String> names = new HashSet<>();
    final List<Dependency> dependencies = new ArrayList<>();

    public PlatformSpec(DependencyHandler handler) {
        this.handler = handler;
    }

    public boolean matches(Collection<String> platforms) {
        return platforms.containsAll(names);
    }

    public void name(String name) {
        names.add(name);
    }

    public void add(Object dependencyNotation) {
        dependencies.add(handler.create(dependencyNotation));
    }
}
