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

package io.github.f2bb.amalgamation.gradle.minecraft;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.util.HashSet;
import java.util.Set;

public class GenericPlatformSpec {

    protected final Project project;
    private final Set<String> names = new HashSet<>();
    private final Set<Dependency> dependencies = new HashSet<>();

    public GenericPlatformSpec(Project project) {
        this.project = project;
    }

    public void name(String name) {
        names.add(name);
    }

    public void add(Object dependencyNotation) {
        dependencies.add(project.getDependencies().create(dependencyNotation));
    }

    public Set<String> getNames() {
        return names;
    }

    public Set<Dependency> getDependencies() {
        return dependencies;
    }
}
