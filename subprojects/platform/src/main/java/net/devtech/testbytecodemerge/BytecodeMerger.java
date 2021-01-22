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

package net.devtech.testbytecodemerge;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import net.devtech.testbytecodemerge.mergers.AccessMerger;
import net.devtech.testbytecodemerge.mergers.InnerClassAttributeMerger;
import net.devtech.testbytecodemerge.mergers.InterfaceMerger;
import net.devtech.testbytecodemerge.mergers.Merger;
import net.devtech.testbytecodemerge.mergers.SignatureMerger;
import net.devtech.testbytecodemerge.mergers.SuperclassMerger;
import net.devtech.testbytecodemerge.mergers.field.FieldMerger;
import net.devtech.testbytecodemerge.mergers.method.MethodMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class BytecodeMerger {
	public static final Logger LOGGER = Logger.getLogger("Merger");
	public static final Merger MERGER;
	static {
		Merger merger = (node, infos) -> {
			ClassNode root = infos.get(0).node;
			node.name = root.name;
			node.version = root.version;
		};
		merger = merger.andThen(new SuperclassMerger());
		merger = merger.andThen(new InterfaceMerger());
		merger = merger.andThen(new AccessMerger());
		merger = merger.andThen(new SignatureMerger());
		merger = merger.andThen(new MethodMerger());
		merger = merger.andThen(new FieldMerger());
		merger = merger.andThen(new InnerClassAttributeMerger());
		MERGER = merger;
	}

	public static void main(String[] args) throws IOException {
		ClassInfo infoA = new ClassInfo(readClass("ClassA"), new String[] {"A"});
		ClassInfo infoB = new ClassInfo(readClass("ClassB"), new String[] {"B"});

		ClassNode merged = new ClassNode();
		MERGER.merge(merged, Arrays.asList(infoA, infoB));

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		merged.accept(writer);

		FileOutputStream output = new FileOutputStream("out.class");
		output.write(writer.toByteArray());
		output.close();
	}

	private static ClassNode readClass(String name) throws IOException {
		FileInputStream fis = new FileInputStream("main/" + name + ".class");
		ClassReader reader = new ClassReader(fis);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);
		fis.close();
		return node;
	}
}
