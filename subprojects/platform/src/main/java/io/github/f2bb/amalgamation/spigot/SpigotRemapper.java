package io.github.f2bb.amalgamation.spigot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.commons.Remapper;

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
			if(mapped == null && (i = internalName.lastIndexOf('$')) != -1) {
				mapped = this.map(internalName.substring(0, i)) + internalName.substring(i);
			}

			if(mapped == null) {
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
