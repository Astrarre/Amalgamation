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

package io.github.f2bb.amalgamation.gradle.plugin.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.annotations.NotNull;

public class BaseAmalgamationGradlePlugin implements Plugin<Project> {
	public static final Gson GSON = new GsonBuilder()
											// todo custom Dependencies serializer
			.create();
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

		IdeaModel ideaModel = (IdeaModel) target.getExtensions().getByName("idea");
		ideaModel.getModule().getExcludeDirs().addAll(target.files(".gradle", "build", ".idea", "out").getFiles());
		ideaModel.getModule().setDownloadJavadoc(true);
		ideaModel.getModule().setDownloadSources(true);
		ideaModel.getModule().setInheritOutputDirs(true);
	}

	protected void registerProvider(Project target) {
		target.getExtensions().create(BaseAmalgamation.class, "ag", BaseAmalgamationImpl.class, target);
	}
}
