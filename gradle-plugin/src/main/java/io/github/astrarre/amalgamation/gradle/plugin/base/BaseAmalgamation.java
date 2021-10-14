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

package io.github.astrarre.amalgamation.gradle.plugin.base;

import java.io.File;
import java.util.function.Supplier;
import io.github.astrarre.amalgamation.gradle.dependencies.DeJiJDependency;
import io.github.astrarre.amalgamation.gradle.ide.eclipse.EclipseExtension;
import io.github.astrarre.amalgamation.gradle.ide.idea.IdeaExtension;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;

public interface BaseAmalgamation {

    <T> Provider<T> provideLazy(Supplier<T> action);

    Dependency deJiJ(String name, Action<DeJiJDependency> configuration);

    /**
     * Adds a url as a direct dependency
     */
    Dependency url(String url);

    /**
     * A utility class for intellij, most useful for generating intellij run configs from gradle tasks.
     * @throws IllegalStateException if idea-ext is not installed
     */
    IdeaExtension idea() throws IllegalStateException;

    /**
     * same idea as {@link #idea()} but u need the "eclipse" plugin
     */
    EclipseExtension eclipse() throws IllegalStateException;
}
