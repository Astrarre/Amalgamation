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

package io.github.f2bb.amalgamation.gradle.plugin.minecraft;

import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamation;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class MinecraftAmalgamationGradlePlugin extends BaseAmalgamationGradlePlugin implements Plugin<Project> {
	public static LauncherMeta getLauncherMeta(Project project) {
		return project.getExtensions().getByType(LauncherMeta.class);
	}

	@Override
	public void apply(@NotNull Project target) {
		super.apply(target);
		target.getRepositories().maven(repository -> {
			repository.setName("Minecraft Libraries");
			repository.setUrl("https://libraries.minecraft.net/");
		});
		target.getExtensions().create(LauncherMeta.class, "launchermeta", LauncherMeta.class, target.getGradle(), target.getLogger());

	}

	@Override
	protected void registerProvider(Project target) {
		target.getExtensions().create(MinecraftAmalgamation.class, "ag", MinecraftAmalgamationImpl.class, target);
	}
}
