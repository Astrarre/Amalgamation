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

import groovy.lang.Closure;
import io.github.f2bb.amalgamation.gradle.base.BaseAmalgamation;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;

import java.util.Collection;

public interface MinecraftAmalgamation extends BaseAmalgamation {

    /**
     * Adds a mappings dependency to use
     *
     * @param dependencyNotation The mappings to use. See {@link DependencyHandler#create(Object)}
     */
    void mappings(Object dependencyNotation);

    /**
     * Adds a Forge based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param dependency       The Forge dependency
     * @param configureClosure Closure to configure this platform
     */
    void forge(String minecraftVersion, Object dependency, Closure configureClosure);

    /**
     * Adds a Forge based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param dependency       The Forge dependency
     * @param configureAction  Action to configure this platform
     */
    void forgeAction(String minecraftVersion, Object dependency, Action<MinecraftPlatformSpec> configureAction);

    /**
     * Adds a Fabric based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param configureClosure Closure to configure this platform
     */
    void fabric(String minecraftVersion, Closure configureClosure);

    /**
     * Adds a Fabric based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param configureAction  Action to configure this platform
     */
    void fabricAction(String minecraftVersion, Action<MinecraftPlatformSpec> configureAction);

    /**
     * Creates the mapping which joins the development mappings to the given target
     *
     * @param target  The target of the mapping set
     * @param version The Minecraft version being used
     * @return A mapping between the development mappings to the target
     */
    MappingSet createMappings(MappingTarget target, String version);

    /**
     * Collects the mapped dependencies which are available for the provided platforms
     *
     * @param platforms The platforms to filter by
     * @return A collection of dependencies
     */
    FileCollection getMappedClasspath(Collection<String> platforms);
}
