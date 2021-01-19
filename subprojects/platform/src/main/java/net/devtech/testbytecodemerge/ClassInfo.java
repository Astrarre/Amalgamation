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

import io.github.f2bb.amalgamation.Platform;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class ClassInfo {
	public final ClassNode node;
	public final String[] names;

	public ClassInfo(ClassNode node, String[] names) {
		this.node = node;
		this.names = names;
	}

	public AnnotationNode createPlatformAnnotation() {
		AnnotationNode node = new AnnotationNode(Type.getDescriptor(Platform.class));
		AnnotationVisitor array = node.visitArray("value");
		for (String s : this.names) {
			array.visit("value", s);
		}
		return node;
	}
}
