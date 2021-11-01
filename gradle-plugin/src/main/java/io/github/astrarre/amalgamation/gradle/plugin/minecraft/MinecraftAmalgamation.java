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
import io.github.astrarre.amalgamation.gradle.dependencies.AssetsDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.CASMergedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.NativesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.aw.AccessWidenerHelper;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.aw.AccessWidenerTransform;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.MappingTarget;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.remap.RemapHelper;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamation;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

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

	default Dependency client(String version) {
		return this.client(version, true);
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @return a clientMappings for the obfuscated client jar
	 */
	Dependency client(String version, boolean split);

	default Dependency server(String version) {
		return this.server(version, true);
	}

	/**
	 * @param strip strip included dependencies
	 */
	default Dependency server(String version, boolean strip) {
		return this.server(version, strip, true);
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @param split split jar into resources and classes jar for speed
	 * @return a clientMappings for the obfuscated server jar (dependencies stripped)
	 */
	Dependency server(String version, boolean strip, boolean split);

	Dependency merged(String version, Action<CASMergedDependency> configurate);

	default Dependency mojmerged(String version, CASMerger.Handler handler, MappingTarget clientMappings) {
		return this.mojmerged(version, handler, true, clientMappings);
	}

	Dependency mojmerged(String version, CASMerger.Handler handler, boolean split, MappingTarget clientMappings);

	default Dependency mojmerged(String version, boolean split, MappingTarget clientMappings) {
		return this.mojmerged(version, CASMerger.FABRIC, split, clientMappings);
	}

	default Dependency mojmerged(String version, MappingTarget clientMappings) {
		return this.mojmerged(version, true, clientMappings);
	}

	default Dependency mojmerged(String version, boolean split) {
		return this.mojmerged(version, CASMerger.FABRIC, split, this.intermediary(version));
	}

	default Dependency mojmerged(String version) {
		return this.mojmerged(version, true, this.intermediary(version));
	}

	default MappingTarget intermediary(String version) {
		return this.mappings("net.fabricmc:intermediary:" + version + ":v2", "official", "intermediary");
	}


	List<Dependency> fabricLoader(String version);

	default Object accessWidener(Action<AccessWidenerHelper> configure) throws IOException {
		return this.transformed(new AccessWidenerTransform(), configure);
	}

	default LibrariesDependency libraries(String version) {
        return this.libraries(version, NOTHING);
	}

	MappingTarget mappings(Object depNotation, String from, String to);

	MappingTarget mappings(Object depNotation, String from, String to, Closure<ModuleDependency> config);

	LibrariesDependency libraries(String version, Action<LibrariesDependency> configure);

	AssetsDependency assets(String version);

	/**
	 * The to string on this clientMappings returns the natives directory
	 */
	NativesDependency natives(String version);

	/**
	 * @param mappings configurate mappings
	 * @return a list of the remapped dependencies
	 */
	Object map(Action<RemapHelper> mappings) throws IOException;

	/**
	 * defaults to the minecraft libraries directory, if it fails, it uses global amalgamation cache/libraries
	 */
	void setLibrariesCache(String directory);

	String librariesCache();
}
