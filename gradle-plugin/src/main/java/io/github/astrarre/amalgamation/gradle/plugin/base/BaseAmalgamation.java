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
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.decomp.DecompileDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.decomp.FabricFernFlowerDecompiler;
import io.github.astrarre.amalgamation.gradle.dependencies.decomp.LoomDecompiler;
import io.github.astrarre.amalgamation.gradle.ide.eclipse.EclipseExtension;
import io.github.astrarre.amalgamation.gradle.ide.idea.IdeaExtension;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.maven.MavenPublication;

public interface BaseAmalgamation {
    /**
     * Reverts Amalg's strange rectified artifact names so that the outputted pom.xml is valid, this should be called after {@link #excludeConfiguration(MavenPublication, Configuration)}
     */
    void fixPom(MavenPublication publication);

    /**
     * Excludes all dependencies in the given configuration from the maven pom / module.json
     *
     * Calling this multiple times for the same publication does work
     */
    void excludeConfiguration(MavenPublication publication, Configuration configuration);

    <T> Provider<T> provideLazy(Supplier<T> action);

    List<File> resolve(Iterable<Dependency> dependency);

    List<File> resolveWithSources(Iterable<Dependency> dependency);

    Provider<FileCollection> sources(Object object);

    Provider<FileCollection> sources(Object object, Closure<ModuleDependency> config);

    //Dependency deJiJ(String name, Action<DeJiJDependency> configuration);

    /**
     * creates a url as a direct dependency
     */
    Dependency url(String url);

    /**
     * A utility class for intellij, most useful for generating intellij run configs from gradle tasks.
     * @throws IllegalStateException if idea is not installed
     */
    IdeaExtension idea() throws IllegalStateException;

    /**
     * same idea as {@link #idea()} but u need the "eclipse" plugin
     */
    EclipseExtension eclipse() throws IllegalStateException;

    default Object fernflower(Object dependency, Action<DecompileDependency> configure) {
        return this.decompile(dependency, new FabricFernFlowerDecompiler(), configure);
    }

    Object decompile(Object dependency, LoomDecompiler decompiler, Action<DecompileDependency> configure);
}
