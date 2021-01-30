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

import io.github.f2bb.amalgamation.gradle.base.GenericPlatformSpec;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class MinecraftPlatformSpec extends GenericPlatformSpec {

    private final Configuration remap;

    public MinecraftPlatformSpec(Project project) {
        super(project);
        remap = project.getConfigurations().detachedConfiguration();
    }

    /**
     * Adds a dependency which should be remapped
     *
     * @param dependencyNotation The dependency. See {@link DependencyHandler#create(Object)}
     */
    public void remap(Object dependencyNotation) {
        remap.getDependencies().add(project.getDependencies().create(dependencyNotation));
    }

    public Configuration getRemap() {
        return remap;
    }
}
