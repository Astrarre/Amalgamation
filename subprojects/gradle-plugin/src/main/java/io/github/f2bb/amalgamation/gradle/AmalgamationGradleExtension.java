/*
 * Amalgamation
 * Copyright (C) 2020 Astrarre
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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AmalgamationGradleExtension {

    private final Project project;
    private final List<Platform> platforms;

    public AmalgamationGradleExtension(Project project) {
        this.project = project;
        this.platforms = new ArrayList<>();
    }

    public void generic(Action<Platform> action) {
        Platform platform = new Platform(project.getDependencies());
        action.execute(platform);
        platforms.add(platform);
    }

    public FileCollection getClasspath(Collection<String> platforms) {
        return project.files(project.provider(() -> {
            ConfigurableFileCollection files = project.files();

            for (Platform platform : this.platforms) {
                if (platforms.containsAll(platform.names)) {
                    files.from(project.getConfigurations().detachedConfiguration(platform.dependencies.toArray(new Dependency[0])).getAsFileTree());
                }
            }

            return files;
        }));
    }

    public FileCollection transform(Object input, Collection<String> platforms) {
        return project.files(project.provider(() -> {
            File root = project.getBuildDir().toPath().resolve("tmp").resolve("amalgamation").resolve(UUID.randomUUID().toString()).toFile();
            ConfigurableFileCollection files = project.files();

            FileTree tree = project.files(input).getAsFileTree();

            tree.visit(details -> {
                File target = details.getRelativePath().getFile(root);

                if (details.getName().endsWith(".class")) {
                    // TODO: Transform
                    details.copyTo(target);
                    System.out.println("Transform " + details.getRelativePath());
                } else {
                    details.copyTo(target);
                }
            });

            return files;
        }));
    }
}
