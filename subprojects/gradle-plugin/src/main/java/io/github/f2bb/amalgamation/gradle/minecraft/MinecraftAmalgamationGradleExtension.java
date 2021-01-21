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
import io.github.f2bb.amalgamation.gradle.minecraft.impl.AmalgamationImpl;
import io.github.f2bb.amalgamation.gradle.minecraft.impl.Fabric;
import io.github.f2bb.amalgamation.gradle.minecraft.impl.Forge;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.util.ConfigureUtil;

import java.util.HashSet;
import java.util.Set;

public class MinecraftAmalgamationGradleExtension {

    private final Project project;
    private Dependency mappings;

    private final Set<Forge> forgeSpecs = new HashSet<>();
    private final Set<Fabric> fabricSpecs = new HashSet<>();
    private final Set<GenericPlatformSpec> genericSpecs = new HashSet<>();
    private Dependency myDependency;

    public MinecraftAmalgamationGradleExtension(Project project) {
        this.project = project;
    }

    /**
     * @return The mapping dependency
     */
    public Dependency getMappings() {
        return mappings;
    }

    /**
     * Sets the mappings dependency to use
     *
     * @param mappings The mappings to use
     */
    public void setMappings(Dependency mappings) {
        this.mappings = mappings;
    }

    /**
     * Sets the mappings dependency to use
     *
     * @param dependencyNotation The mappings to use. See {@link DependencyHandler#create(Object)}
     */
    public void mappings(Object dependencyNotation) {
        setMappings(project.getDependencies().create(dependencyNotation));
    }

    /**
     * Adds a Forge based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param dependency       The Forge dependency
     * @param configureClosure Closure to configure this platform
     */
    public void forge(String minecraftVersion, Object dependency, Closure configureClosure) {
        forgeAction(minecraftVersion, dependency, spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    /**
     * Adds a Forge based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param dependency       The Forge dependency
     * @param configureAction  Action to configure this platform
     */
    public void forgeAction(String minecraftVersion, Object dependency, Action<MinecraftPlatformSpec> configureAction) {
        assertMutable();

        MinecraftPlatformSpec forge = new MinecraftPlatformSpec(project);
        forge.name("forge");
        forge.name(minecraftVersion);
        configureAction.execute(forge);

        forgeSpecs.add(new Forge(minecraftVersion, project.getDependencies().create(dependency), forge));
    }

    /**
     * Adds a Fabric based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param configureClosure Closure to configure this platform
     */
    public void fabric(String minecraftVersion, Closure configureClosure) {
        fabricAction(minecraftVersion, spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    /**
     * Adds a Fabric based platform
     *
     * @param minecraftVersion The Minecraft version
     * @param configureAction  Action to configure this platform
     */
    public void fabricAction(String minecraftVersion, Action<MinecraftPlatformSpec> configureAction) {
        assertMutable();

        MinecraftPlatformSpec fabric = new MinecraftPlatformSpec(project);
        fabric.name("fabric");
        fabric.name(minecraftVersion);
        configureAction.execute(fabric);

        fabricSpecs.add(new Fabric(minecraftVersion, fabric));
    }

    /**
     * Adds a generic platform
     *
     * @param configureClosure Closure to configure this platform
     */
    public void generic(Closure configureClosure) {
        genericAction(spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    /**
     * Adds a generic platform
     *
     * @param configureAction Action to configure this platform
     */
    public void genericAction(Action<GenericPlatformSpec> configureAction) {
        assertMutable();

        GenericPlatformSpec spec = new GenericPlatformSpec(project);
        configureAction.execute(spec);

        if (spec.getNames().isEmpty()) {
            throw new IllegalStateException("No names were given to this platform");
        }

        if (spec.getDependencies().isEmpty()) {
            throw new IllegalStateException("No dependencies were given to this platform");
        }

        genericSpecs.add(spec);
    }

    /**
     * Freezes this extension and creates the merged dependency
     *
     * @return A dependency which can be added to a configuration
     */
    public Dependency create() {
        if (myDependency != null) {
            return myDependency;
        }

        return myDependency = AmalgamationImpl.createDependencyFromMatrix(project, mappings, forgeSpecs, fabricSpecs, genericSpecs);
    }

    private void assertMutable() {
        if (myDependency != null) {
            throw new IllegalStateException("Dependency matrix is frozen");
        }
    }

}
