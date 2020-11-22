/*
 * Amalgamation
 * Copyright (C) 2020 IridisMC
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

import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

public class AmalgamationGradleExtension {

    private final Project project;

    public AmalgamationGradleExtension(Project project) {
        this.project = project;
    }

    public void usePlatforms(JavaCompile task, String... platforms) {
        // TODO: Configure that JavaCompile task to use the given platforms. Usage:
        //  val myCompile by tasks.registering<JavaCompile> {
        //      amalgamation.usePlatforms(this, "a", "b")
        //  }
    }
}
