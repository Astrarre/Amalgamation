/*
 * Copyright (c) 2016, 2018, Player, asie
 * Copyright (c) 2021, FabricMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.astrarre.amalgamation.gradle.dependencies.remap.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.ZipRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Lazy;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.VirtualZipEntry;
import org.objectweb.asm.commons.Remapper;

public class MetaInfFixerImpl implements AmalgRemapper {
	private Remapper remapper;

	@Override
	public ZipRemapper createNew() {
		return (entry, isClasspath) -> {
			if(this.canTransform(entry.path())) {
				try {
					return this.visitEntry(entry, isClasspath);
				} catch(IOException e) {
					System.err.println("Error in fixing " + entry.path());
					e.printStackTrace();
					return true;
				}
			}
			return false;
		};
	}

	@Override
	public void hash(Hasher hasher) {
		hasher.putString("META_INF fixer", StandardCharsets.UTF_8);
	}

	private boolean visitEntry(VirtualZipEntry entry, boolean isClasspath) throws IOException {
		String[] names = names(entry.path());
		Lazy<ByteArrayInputStream> bis = new Lazy<>(() -> {
			ByteBuffer buffer = entry.read();
			return new ByteArrayInputStream(buffer.array(), buffer.arrayOffset(), buffer.limit());
		});
		if(names.length == 2 && names[names.length - 1].equals("MANIFEST.MF")) {
			Manifest manifest = new Manifest(bis.get());
			fixManifest(manifest, remapper);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			manifest.write(baos);
			entry.writeToOutput(ByteBuffer.wrap(baos.toByteArray()));
		} else if(names.length == 3 && names[1].equals("services")) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(bis.get())); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos))) {
				fixServiceDecl(reader, writer, remapper);
			}
			entry.writeToOutput(ByteBuffer.wrap(baos.toByteArray()));
		}
		return true;
	}

	private static String mapFullyQualifiedClassName(String name, Remapper tr) {
		assert name.indexOf('/') < 0;

		return tr.map(name.replace('.', '/')).replace('/', '.');
	}

	public boolean canTransform(String relativePath) {
		String[] names;
		return relativePath.startsWith("META-INF")
				&& (shouldStripForFixMeta(relativePath)
				    || (names = names(relativePath)).length == 2 && names[1].equals("MANIFEST.MF")
				    || names.length == 3 && names[1].equals("services"));
	}

	private static boolean shouldStripForFixMeta(String file) {
		String[] names = names(file);
		if (names.length != 2) return false; // not directly inside META-INF dir

		String fileName = names[names.length - 1];

		// https://docs.oracle.com/en/java/javase/12/docs/specs/jar/jar.html#signed-jar-file
		return fileName.endsWith(".SF")
				|| fileName.endsWith(".DSA")
				|| fileName.endsWith(".RSA")
				|| fileName.startsWith("SIG-");
	}


	private static void fixManifest(Manifest manifest, Remapper remapper) {
		Attributes mainAttrs = manifest.getMainAttributes();

		if (remapper != null) {
			String val = mainAttrs.getValue(Attributes.Name.MAIN_CLASS);
			if (val != null) mainAttrs.put(Attributes.Name.MAIN_CLASS, mapFullyQualifiedClassName(val, remapper));

			val = mainAttrs.getValue("Launcher-Agent-Class");
			if (val != null) mainAttrs.put("Launcher-Agent-Class", mapFullyQualifiedClassName(val, remapper));
		}

		mainAttrs.remove(Attributes.Name.SIGNATURE_VERSION);

		for (Iterator<Attributes> it = manifest.getEntries().values().iterator(); it.hasNext(); ) {
			Attributes attrs = it.next();

			for (Iterator<Object> it2 = attrs.keySet().iterator(); it2.hasNext(); ) {
				Attributes.Name attrName = (Attributes.Name) it2.next();
				String name = attrName.toString();

				if (name.endsWith("-Digest") || name.contains("-Digest-") || name.equals("Magic")) {
					it2.remove();
				}
			}

			if (attrs.isEmpty()) it.remove();
		}
	}

	private static void fixServiceDecl(BufferedReader reader, BufferedWriter writer, Remapper remapper) throws IOException {
		String line;

		while ((line = reader.readLine()) != null) {
			int end = line.indexOf('#');
			if (end < 0) end = line.length();

			// trim start+end to skip ' ' and '\t'

			int start = 0;
			char c;

			while (start < end && ((c = line.charAt(start)) == ' ' || c == '\t')) {
				start++;
			}

			while (end > start && ((c = line.charAt(end - 1)) == ' ' || c == '\t')) {
				end--;
			}

			if (start == end) {
				writer.write(line);
			} else {
				writer.write(line, 0, start);
				writer.write(mapFullyQualifiedClassName(line.substring(start, end), remapper));
				writer.write(line, end, line.length() - end);
			}

			writer.newLine();
		}
	}

	@Override
	public boolean requiresClasspath() {
		return false;
	}

	@Override
	public boolean hasPostStage() {
		return false;
	}

	@Override
	public void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper) {
		this.remapper = remapper;
	}

	private static final String[] EMPTY = {};
	static String[] names(String path) {
		if(path.isEmpty()) {
			return EMPTY;
		}
		if(path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if(path.isEmpty()) {
			return EMPTY;
		}
		if(path.charAt(path.length()-1) == '/') {
			path = path.substring(0, path.length()-1);
		}
		if(path.isEmpty()) {
			return EMPTY;
		}
		return path.split("/");
	}
}
