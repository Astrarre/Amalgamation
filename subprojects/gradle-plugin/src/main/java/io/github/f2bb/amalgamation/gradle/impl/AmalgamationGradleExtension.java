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

import groovy.lang.Closure;
import io.github.f2bb.amalgamation.gradle.base.GenericPlatformSpec;
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftAmalgamation;
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftPlatformSpec;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.util.ConfigureUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AmalgamationGradleExtension implements MinecraftAmalgamation {

    private final Project project;

    private final Set<GenericPlatformSpec> genericSpecs = new HashSet<>();
    private final Set<Forge> forgeSpecs = new HashSet<>();
    private final Set<Fabric> fabricSpecs = new HashSet<>();
    private final Configuration mappings;

    private Dependency myDependency;

    public AmalgamationGradleExtension(Project project) {
        this.project = project;
        mappings = project.getConfigurations().detachedConfiguration();
    }

    @Override
    public void mappings(Object dependencyNotation) {
        mappings.getDependencies().add(project.getDependencies().create(dependencyNotation));
    }

    @Override
    public void generic(Closure configureClosure) {
        genericAction(spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    @Override
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

    @Override
    public void forge(String minecraftVersion, Object dependency, Closure configureClosure) {
        forgeAction(minecraftVersion, dependency, spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    @Override
    public void forgeAction(String minecraftVersion, Object dependency, Action<MinecraftPlatformSpec> configureAction) {
        assertMutable();

        MinecraftPlatformSpec forge = new MinecraftPlatformSpec(project);
        forge.name("forge");
        forge.name(minecraftVersion);
        configureAction.execute(forge);

        forgeSpecs.add(new Forge(project, minecraftVersion, project.getDependencies().create(dependency), forge));
    }

    @Override
    public void fabric(String minecraftVersion, Closure configureClosure) {
        fabricAction(minecraftVersion, spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    @Override
    public void fabricAction(String minecraftVersion, Action<MinecraftPlatformSpec> configureAction) {
        assertMutable();

        MinecraftPlatformSpec fabric = new MinecraftPlatformSpec(project);
        fabric.name("fabric");
        fabric.name(minecraftVersion);
        configureAction.execute(fabric);

        fabricSpecs.add(new Fabric(project, minecraftVersion, fabric));
    }

    @Override
    public Dependency create() throws IOException {
        if (myDependency != null) {
            return myDependency;
        }

        return myDependency = AmalgamationImpl.createDependencyFromMatrix(project, mappings, forgeSpecs, fabricSpecs, genericSpecs);
    }

    @Override
    public FileCollection getClasspath(Collection<String> platforms) {
        Configuration classpath = project.getConfigurations().detachedConfiguration();

        for (Forge spec : forgeSpecs) {
            if (spec.forge.getNames().containsAll(platforms)) {
                classpath.extendsFrom(spec.forge.getDependencies());
                classpath.extendsFrom(spec.forge.getRemap());
            }
        }

        for (Fabric spec : fabricSpecs) {
            if (spec.fabric.getNames().containsAll(platforms)) {
                classpath.extendsFrom(spec.fabric.getDependencies());
                classpath.extendsFrom(spec.fabric.getRemap());
            }
        }

        for (GenericPlatformSpec spec : genericSpecs) {
            if (spec.getNames().containsAll(platforms)) {
                classpath.extendsFrom(spec.getDependencies());
            }
        }

        return classpath.getAsFileTree();
    }

    protected void assertMutable() {
        if (myDependency != null) {
            throw new IllegalStateException("Dependency matrix is frozen");
        }
    }
}
