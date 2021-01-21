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
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.util.ConfigureUtil;

public class MinecraftAmalgamationGradleExtension {

    private final Project project;
    private Dependency mappings;

    public MinecraftAmalgamationGradleExtension(Project project) {
        this.project = project;
    }

    public Dependency getMappings() {
        return mappings;
    }

    public void setMappings(Dependency mappings) {
        this.mappings = mappings;
    }

    public void mappings(Object dependencyNotation) {
        setMappings(project.getDependencies().create(dependencyNotation));
    }

    public void forge(String minecraftVersion, Object dependency, Closure configureClosure) {
        forgeAction(minecraftVersion, dependency, spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    public void forgeAction(String minecraftVersion, Object dependency, Action<MinecraftPlatformSpec> configureAction) {
    }

    public void fabric(String minecraftVersion, Closure configureClosure) {
        fabricAction(minecraftVersion, spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    public void fabricAction(String minecraftVersion, Action<MinecraftPlatformSpec> configureAction) {
    }

    public void generic(Closure configureClosure) {
        genericAction(spec -> {
            ConfigureUtil.configure(configureClosure, spec);
        });
    }

    public void genericAction(Action<GenericPlatformSpec> configureAction) {

    }

    public void install() {
    }
}
