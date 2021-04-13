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

package io.github.f2bb.amalgamation.platform.util;

import java.util.Set;

import io.github.f2bb.amalgamation.Displace;
import io.github.f2bb.amalgamation.Parent;
import io.github.f2bb.amalgamation.Platform;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class ClassInfo {
	public static final String PLATFORM = Type.getDescriptor(Platform.class);
	public static final String DISPLACE = Type.getDescriptor(Displace.class);
	public static final String PARENT = Type.getDescriptor(Parent.class);
	public final ClassNode node;
	public final Set<String> names;

	public ClassInfo(ClassNode node, Set<String> names) {
		this.node = node;
		this.names = names;
	}

	public static AnnotationNode displace(String name) {
		AnnotationNode node = new AnnotationNode(DISPLACE);
		node.visit("value", name);
		node.visitEnd();
		return node;
	}

	public static AnnotationNode createPlatformAnnotation(Iterable<String> names) {
		AnnotationNode node = new AnnotationNode(PLATFORM);
		AnnotationVisitor array = node.visitArray("value");
		for (String s : names) {
			array.visit("value", s);
		}
		return node;
	}

	public AnnotationNode createPlatformAnnotation() {
		return createPlatformAnnotation(this.names);
	}

	public ClassNode getNode() {
		return this.node;
	}
}
