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

import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.RemappingDependency;
import io.github.astrarre.amalgamation.gradle.files.assets.Assets;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamation;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;

// todo support looms caches
// todo natives (dlls)
// todo assets

@SuppressWarnings ({
		"rawtypes",
		"unchecked"
})
public interface MinecraftAmalgamation extends BaseAmalgamation {
	Action NOTHING = o -> {};
	static <T> Action<T> nothing() {
		return NOTHING;
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @return a dependency for the obfuscated client jar
	 */
	Dependency client(String version);

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @return a dependency for the obfuscated server jar (dependencies stripped)
	 */
	Dependency server(String version);

	default Dependency libraries(String version) {
        return this.libraries(version, NOTHING);
	}

	Dependency libraries(String version, Action<LibrariesDependency> configure);

	Assets assets(String version);

	String natives(String version);

	/**
	 * @param mappings configurate mappings
	 * @return a list of the remapped dependencies
	 */
	Dependency map(Action<RemappingDependency> mappings);

	/**
	 * defaults to the minecraft libraries directory, if it fails, it uses <\global amalgamation cache>/libraries
	 */
	void setLibrariesCache(String directory);

	String librariesCache();
}
