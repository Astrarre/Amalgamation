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

import io.github.f2bb.amalgamation.gradle.impl.AmalgamationGradleExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class BaseAmalgamationGradlePlugin implements Plugin<Project> {
	public static boolean refreshDependencies;

	@Override
	public void apply(@NotNull Project target) {
		System.out.println("CACHE LOCATION AAAA    " + target.getGradle().getGradleHomeDir() + "   " + target.getGradle().getGradleUserHomeDir());
		refreshDependencies = target.getGradle().getStartParameter().isRefreshDependencies();
		target.getExtensions().create(BaseAmalgamation.class, "amalgamation", AmalgamationGradleExtension.class, target);
	}
}
