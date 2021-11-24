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

package io.github.astrarre.amalgamation.gradle.plugin.minecraft;

import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.astrarre.amalgamation.gradle.tasks.remap.MigrateSourcesTask;
import io.github.astrarre.amalgamation.gradle.tasks.remap.RemapJar;
import io.github.astrarre.amalgamation.gradle.tasks.remap.RemapSourcesJar;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.NotNull;

public class MinecraftAmalgamationGradlePlugin extends BaseAmalgamationGradlePlugin implements Plugin<Project> {
	private static LauncherMeta instance;
	public static LauncherMeta getLauncherMeta(Project project) {
		if(instance == null) {
			instance = new LauncherMeta(AmalgIO.globalCache(project), project);
		}
		return instance;
	}

	public static String getLibrariesCache(Project project) {
		return project.getExtensions().getByType(MinecraftAmalgamation.class).librariesCache();
	}

	@Override
	public void apply(@NotNull Project target) {
		super.apply(target);
		target.getRepositories().maven(repository -> {
			repository.setName("Minecraft Libraries");
			repository.setUrl("https://libraries.minecraft.net/");
		});
		ExtensionContainer container = target.getExtensions();
		container.create(LauncherMeta.class, "launchermeta", LauncherMeta.class, AmalgIO.globalCache(target), target);
		container.add(new TypeOf<>() {}, "RemapJar", RemapJar.class);
		container.add(new TypeOf<>() {}, "RemapSourcesJar", RemapSourcesJar.class);
		container.add(new TypeOf<>() {}, "MigrateSourcesTask", MigrateSourcesTask.class);
	}

	@Override
	protected void registerProvider(Project target) {
		MinecraftAmalgamation register = this.register(target, MinecraftAmalgamation.class, MinecraftAmalgamationImpl.class);
	}
}
