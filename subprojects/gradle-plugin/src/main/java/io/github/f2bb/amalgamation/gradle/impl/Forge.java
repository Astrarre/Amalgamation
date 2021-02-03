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

package io.github.f2bb.amalgamation.gradle.impl;

import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftPlatformSpec;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.nio.file.Path;
import java.util.List;

class Forge {

    private final Project project;
    final String minecraftVersion;
    final Dependency installer;
    final MinecraftPlatformSpec forge;

    public Forge(Project project, String minecraftVersion, Dependency installer, MinecraftPlatformSpec forge) {
        this.project = project;
        this.minecraftVersion = minecraftVersion;
        this.installer = installer;
        this.forge = forge;
    }

    public List<Path> getFiles(MappingSet mappings) {
        throw new UnsupportedOperationException();
    }
}
