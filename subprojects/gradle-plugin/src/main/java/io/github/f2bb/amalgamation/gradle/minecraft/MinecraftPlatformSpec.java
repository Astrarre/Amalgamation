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
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.HashSet;
import java.util.Set;

public class MinecraftPlatformSpec extends GenericPlatformSpec {

    private final Set<Dependency> remap = new HashSet<>();

    public MinecraftPlatformSpec(Project project) {
        super(project);
    }

    /**
     * Adds a dependency which should be remapped
     *
     * @param dependencyNotation The dependency. See {@link DependencyHandler#create(Object)}
     */
    public void remap(Object dependencyNotation) {
        remap.add(project.getDependencies().create(dependencyNotation));
    }

    public Set<Dependency> getRemap() {
        return remap;
    }
}
