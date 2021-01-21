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

package io.github.f2bb.amalgamation.gradle.base;

import groovy.lang.Closure;
import io.github.f2bb.amalgamation.gradle.impl.AmalgamationImpl;
import io.github.f2bb.amalgamation.gradle.minecraft.GenericPlatformSpec;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.util.ConfigureUtil;

import java.util.HashSet;
import java.util.Set;

public class BaseAmalgamationGradleExtension {

    private final Project project;
    private final Set<GenericPlatformSpec> genericSpecs = new HashSet<>();
    private Dependency myDependency;

    public BaseAmalgamationGradleExtension(Project project) {
        this.project = project;
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
        if (myDependency != null) {
            throw new IllegalStateException("Dependency matrix is frozen");
        }

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

        return myDependency = AmalgamationImpl.createDependencyFromMatrix(project, genericSpecs);
    }
}
