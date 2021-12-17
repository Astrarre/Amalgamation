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

import java.io.IOException;
import java.util.List;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.AccessWidenerDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.AssetsDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.CASMergedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.NativesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.RemapDependencyConfig;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.MappingTarget;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamation;
import io.github.astrarre.amalgamation.gradle.utils.json.Json;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.provider.Provider;

// todo support looms caches

@SuppressWarnings ({
		"rawtypes",
		"unchecked"
})
public interface MinecraftAmalgamation extends BaseAmalgamation {
	Action NOTHING = o -> {};
	static <T> Action<T> nothing() {
		return NOTHING;
	}

	default Object client(String version) {
		return this.client(version, true);
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @return a clientMappings for the obfuscated client jar
	 */
	Object client(String version, boolean split);

	default Object server(String version) {
		return this.server(version, true);
	}

	/**
	 * @param strip strip included dependencies
	 */
	default Object server(String version, boolean strip) {
		return this.server(version, strip, true);
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @param split split jar into resources and classes jar for speed
	 * @return a clientMappings for the obfuscated server jar (dependencies stripped)
	 */
	Object server(String version, boolean strip, boolean split);

	Object merged(String version, Action<CASMergedDependency> configurate);

	default Object mojmerged(String version, SideAnnotationHandler handler, MappingTarget clientMappings) {
		return this.mojmerged(version, handler, true, clientMappings);
	}

	Object mojmerged(String version, SideAnnotationHandler handler, boolean split, MappingTarget clientMappings);

	default Object mojmerged(String version, boolean split, MappingTarget clientMappings) {
		return this.mojmerged(version, SideAnnotationHandler.FABRIC, split, clientMappings);
	}

	default Object mojmerged(String version, MappingTarget clientMappings) {
		return this.mojmerged(version, true, clientMappings);
	}

	default Object mojmerged(String version, boolean split) {
		return this.mojmerged(version, SideAnnotationHandler.FABRIC, split, this.intermediary(version));
	}

	default Object mojmerged(String version) {
		return this.mojmerged(version, true, this.intermediary(version));
	}

	default MappingTarget intermediary(String version) {
		return this.mappings("net.fabricmc:intermediary:" + version + ":v2", "official", "intermediary");
	}

	List<Dependency> fabricLoader(String version);

	Object accessWidener(Object depNotation, Action<AccessWidenerDependency> configure) throws IOException;

	default Object libraries(String version) {
        return this.libraries(version, NOTHING);
	}

	MappingTarget mappings(Object depNotation, String from, String to);

	MappingTarget mappings(Object depNotation, String from, String to, Closure<ModuleDependency> config);

	Object libraries(String version, Action<LibrariesDependency> configure);

	AssetsDependency assets(String version);

	/**
	 * The to string on this clientMappings returns the natives directory
	 */
	String natives(String version);

	/**
	 * @param mappings configurate mappings
	 * @return a list of the remapped dependencies
	 */
	Object map(Action<RemapDependencyConfig> mappings) throws IOException;

	/**
	 * defaults to the minecraft libraries directory, if it fails, it uses global amalgamation cache/libraries
	 */
	void setLibrariesCache(String directory);

	String librariesCache();
}
