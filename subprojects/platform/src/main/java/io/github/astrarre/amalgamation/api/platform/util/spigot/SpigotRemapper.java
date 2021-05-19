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

package io.github.astrarre.amalgamation.api.platform.util.spigot;

import org.objectweb.asm.commons.Remapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class SpigotRemapper extends Remapper {
	private static final String PACKAGE_REGEX = "net/minecraft/server/v\\d*_\\d*_R\\d*/";
	/**
	 * named -> obf
	 */
	private final Map<String, String> classMappings = new HashMap<>(), fieldMappings = new HashMap<>(),
			methodMappings = new HashMap<>();

	public SpigotRemapper(File classes, File members) throws IOException {
		this(Files.newBufferedReader(classes.toPath()), Files.newBufferedReader(members.toPath()));
	}

	public SpigotRemapper(BufferedReader classes, BufferedReader members) throws IOException {
		String line;
		while ((line = classes.readLine()) != null) {
			if (!line.isEmpty()) {
				if (line.charAt(0) != '#') {
					String[] split = line.split(" ");
					// named -> obf
					this.classMappings.put(split[1], split[0]);
				}
			}
		}

		while ((line = members.readLine()) != null) {
			if (!line.isEmpty()) {
				if (line.charAt(0) != '#') {
					String[] split = line.split(" ");
					if (split.length == 4) { // method mapping, [owner, obf, desc, name]
						// owner;name;desc
						this.methodMappings.put(split[0] + ';' + split[3] + ';' + split[2], split[1]);
					} else { // field mapping: [owner, obf, name]
						this.fieldMappings.put(split[0] + ';' + split[2], split[1]);
					}
				}
			}
		}
	}

	@Override
	public String map(String internalName) {
		if (internalName.startsWith("net/minecraft/server/v")) {
			String name = internalName.replaceAll(PACKAGE_REGEX, "");
			String mapped = this.classMappings.get(name);
			int i;
			if (mapped == null && (i = internalName.lastIndexOf('$')) != -1) {
				mapped = this.map(internalName.substring(0, i)) + internalName.substring(i);
			}

			if (mapped == null) {
				mapped = internalName;
			}
			return mapped;
		}
		return super.map(internalName);
	}

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		// we need to nuke the weird package names in the descriptor
		return this.methodMappings
				.getOrDefault(this.mapType(owner) + ';' + name + ';' + descriptor.replaceAll(PACKAGE_REGEX, ""),
						super.mapMethodName(owner, name, descriptor));
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		return this.fieldMappings
				.getOrDefault(this.mapType(owner) + ';' + name, super.mapFieldName(owner, name, descriptor));
	}
}
