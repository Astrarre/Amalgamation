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

import io.github.astrarre.amalgamation.gradle.ide.IdeExtension;
import io.github.astrarre.amalgamation.gradle.ide.intellij.ConfigureIntellij;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.annotations.NotNull;

public class BaseAmalgamationGradlePlugin implements Plugin<Project> {
	public static boolean refreshDependencies, offlineMode, refreshAmalgamationCaches;

	@Override
	public void apply(@NotNull Project target) {
		StartParameter parameter = target.getGradle().getStartParameter();
		refreshDependencies = parameter.isRefreshDependencies();
		if(refreshDependencies) {
			refreshAmalgamationCaches = true;
		} else {
			refreshAmalgamationCaches = Boolean.getBoolean("refreshAmalgamationCaches");
		}
		offlineMode = parameter.isOffline();
		this.registerProvider(target);

		// target.getGradle().buildFinished(result -> {});
		target.getExtensions().create(IdeExtension.class, "ide", IdeExtension.class);

		PluginContainer plugins = target.getPlugins();
		IdeaPlugin plugin = plugins.findPlugin(IdeaPlugin.class);
		if(plugin != null) {
			ConfigureIntellij.configure(target, plugin.getModel());
		} else {
			plugins.whenPluginAdded(p -> {
				if(p instanceof IdeaPlugin d) {
					ConfigureIntellij.configure(target, d.getModel());
				}
			});
		}
	}

	protected void registerProvider(Project target) {
		target.getExtensions().create(BaseAmalgamation.class, "ag", BaseAmalgamationImpl.class, target);
	}
}
