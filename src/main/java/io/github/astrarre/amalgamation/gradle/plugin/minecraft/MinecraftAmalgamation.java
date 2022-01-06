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

import java.util.List;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.dependencies.AssetsDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.CASMergedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamation;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;

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

	default Set<Object> client(String version) {
		return this.client(version, true);
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @return a clientMappings for the obfuscated client jar
	 */
	Set<Object> client(String version, boolean split);

	default Set<Object> server(String version) {
		return this.server(version, true);
	}

	/**
	 * @param strip strip included dependencies
	 */
	default Set<Object> server(String version, boolean strip) {
		return this.server(version, strip, true);
	}

	/**
	 * @param version the minecraft version string, should match up with launchermeta
	 * @param split split jar into resources and classes jar for speed
	 * @return a clientMappings for the obfuscated server jar (dependencies stripped)
	 */
	Set<Object> server(String version, boolean strip, boolean split);

	Set<Object> merged(String version, Action<CASMergedDependency> configurate);

	/**
	 * "MojMerger" is short for "Mojang Mappings Merger", and it's functionality is similar to that of the CAS Merger.
	 * However, instead of requiring the server jar, it instead downloads the official deobfuscation mappings for the server
	 * provided by mojang, and uses that to determine what members are present on the server.
	 * It then uses a clientside mapping to determine what members are present on the client, this is to
	 * avoid needing to read the entire jar first, since mappings have to be parsed later on anyways (remapping)
	 * This is much faster than CASMerging, and should be used when preferable. MojMerge provided jars are also
	 * automatically "split" in order to improve remapping performance.
	 */
	default Set<Object> mojmerged(String version, SideAnnotationHandler handler, MappingTarget clientMappings) {
		return this.mojmerged(version, handler, true, clientMappings);
	}

	Set<Object> mojmerged(String version, SideAnnotationHandler handler, boolean split, MappingTarget clientMappings);

	MappingTarget mojmap(String version, boolean isClient);

	default Set<Object> mojmerged(String version, boolean split, MappingTarget clientMappings) {
		return this.mojmerged(version, SideAnnotationHandler.FABRIC, split, clientMappings);
	}

	default Set<Object> mojmerged(String version, MappingTarget clientMappings) {
		return this.mojmerged(version, true, clientMappings);
	}

	default Set<Object> mojmerged(String version, boolean split) {
		return this.mojmerged(version, SideAnnotationHandler.FABRIC, split, this.intermediary(version));
	}

	default Set<Object> mojmerged(String version) {
		return this.mojmerged(version, true, this.intermediary(version));
	}

	default MappingTarget intermediary(String version) {
		return this.mappings("net.fabricmc:intermediary:" + version + ":v2", "official", "intermediary");
	}

	List<Dependency> fabricLoader(String version);

	default Set<Object> libraries(String version) {
        return this.libraries(version, NOTHING);
	}

	Set<Object> libraries(String version, Action<LibrariesDependency> configure);

	Object gradleFriendlyLibraries(String version);

	AssetsDependency assets(String version);

	/**
	 * The to string on this clientMappings returns the natives directory
	 */
	String natives(String version);

	/**
	 * defaults to the minecraft libraries directory, if it fails, it uses global amalgamation cache/libraries
	 */
	void setLibrariesCache(String directory);

	String librariesCache();
}
