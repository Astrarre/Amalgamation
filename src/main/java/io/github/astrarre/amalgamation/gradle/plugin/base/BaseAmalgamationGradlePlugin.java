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

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import io.github.astrarre.amalgamation.gradle.ide.eclipse.ConfigureEclipse;
import io.github.astrarre.amalgamation.gradle.ide.idea.ConfigIdea;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import io.github.astrarre.amalgamation.gradle.utils.zip.ZipIO;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.PluginContainer;
import org.jetbrains.annotations.NotNull;

public class BaseAmalgamationGradlePlugin implements Plugin<Project> {
	public static Gradle gradle;

	public static boolean refreshDependencies, offlineMode, refreshAmalgamationCaches;

	@Override
	public void apply(@NotNull Project target) {
		for(AmalgDirs dirs : List.of(AmalgDirs.ROOT_PROJECT, AmalgDirs.GLOBAL)) {
			for(Path dir : List.of(dirs.aws(target), dirs.remaps(target), dirs.decomps(target), dirs.unpack(target))) {
				target.getRepositories().maven(repository -> {
					repository.setName("Amalgamation " + dirs.name() + " cache");
					repository.setUrl(dir.toUri());
				});
			}
		}

		if(gradle == null) {
			Gradle gradle = BaseAmalgamationGradlePlugin.gradle = target.getGradle();
			target.getLogger().lifecycle("Initializing Amalgamation Global Gradle State");
			
			StartParameter parameter = target.getGradle().getStartParameter();
			refreshDependencies = parameter.isRefreshDependencies();
			if(refreshDependencies) {
				refreshAmalgamationCaches = true;
			} else {
				refreshAmalgamationCaches = Boolean.getBoolean("refreshAmalgamationCaches");
			}
			
			if(refreshAmalgamationCaches) {
				target.getLogger().warn("Refresh Amalgamation Caches Enabled: Build times may suffer.");
			}
			offlineMode = parameter.isOffline();
			
			var temp = new Object() {
				Plugin<Project> plugin;
			};
			
			listenFor(target, "idea", idea -> {
				ConfigIdea.configure(target, idea);
				temp.plugin = idea;
			});
			listenFor(target, "eclipse", eclipse -> ConfigureEclipse.configure(target));
			
			gradle.buildFinished(result -> ZipIO.nuclearOption());
		}
		
		this.registerProvider(target);
	}

	public static void listenFor(Project target, String id, Consumer<Plugin<Project>> onFound) {
		PluginContainer plugins = target.getPlugins();
		plugins.withId(id, onFound::accept);
	}

	protected void registerProvider(Project target) {
		this.register(target, BaseAmalgamation.class, BaseAmalgamationImpl.class);
	}

	protected <T> T register(Project target, Class<T> extensionType, Class<? extends T> realType) {
		return target.getExtensions().create(extensionType, "ag", realType, target);
	}
}
