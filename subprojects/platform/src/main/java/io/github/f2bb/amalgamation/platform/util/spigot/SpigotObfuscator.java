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

package io.github.f2bb.amalgamation.platform.util.spigot;

import io.github.f2bb.amalgamation.platform.util.PlatformUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * remaps the spigot jar back to obfuscated names
 */
public class SpigotObfuscator {
	private static final ThreadLocal<byte[]> BUFFERS = ThreadLocal.withInitial(() -> new byte[4096]);

	/**
	 * @param buildata the buildata folder outputted by buildtools
	 * @param spigot   the spigot jar
	 */
	public static void remap(File buildata, File spigot, File output) throws IOException {
		SpigotRemapper remapper = new SpigotRemapper(
				new File(buildata, "mappings/bukkit-" + PlatformUtil.MINECRAFT_VERSION + "-cl.csrg"),
				new File(buildata, "mappings/bukkit-" + PlatformUtil.MINECRAFT_VERSION + "-members.csrg")
		);

		ZipFile file = new ZipFile(spigot);
		Enumeration<? extends ZipEntry> enumeration = file.entries();
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(output));
		while (enumeration.hasMoreElements()) {
			ZipEntry entry = enumeration.nextElement();
			InputStream in = file.getInputStream(entry);
			if (entry.getName().endsWith(".class")) {
				ClassReader reader = new ClassReader(in);
				ClassWriter writer = new ClassWriter(0);
				ClsRmpr remap = new ClsRmpr(writer, remapper);
				reader.accept(remap, 0);
				out.putNextEntry(new ZipEntry(remap.getName() + ".class"));
				out.write(writer.toByteArray());
			} else {
				out.putNextEntry(entry);
				to(in, out);
			}
			out.closeEntry();
		}
		out.close();
	}

	public static void main(String[] args) throws IOException {
		remap(new File("ohno/BuildData"), new File("ohno/out/spigot-1.16.4.jar"), new File("ohno/obf.jar"));
	}

	private static void to(InputStream in, OutputStream out) throws IOException {
		byte[] buf = BUFFERS.get();
		int read;
		while ((read = in.read(buf)) != -1) {
			out.write(buf, 0, read);
		}
	}

	private static class ClsRmpr extends ClassRemapper {
		public ClsRmpr(ClassWriter writer, SpigotRemapper remapper) {
			super(writer, remapper);
		}

		public String getName() {
			return this.remapper.mapType(this.className);
		}

		// todo lambda remapping
	}
}
