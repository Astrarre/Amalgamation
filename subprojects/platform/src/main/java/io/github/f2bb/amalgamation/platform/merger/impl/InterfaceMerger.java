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

package io.github.f2bb.amalgamation.platform.merger.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.f2bb.amalgamation.Interface;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

class InterfaceMerger implements Merger {
	public static final String INTERFACE = Type.getDescriptor(Interface.class);

	@Override
	public void merge(MergerConfig mergerConfig) {
		Map<String, List<ClassInfo>> interfaces = new HashMap<>();
		for (ClassInfo info : mergerConfig.getInfos()) {
			for (String anInterface : info.node.interfaces) {
				interfaces.computeIfAbsent(anInterface, s -> new ArrayList<>()).add(info);
			}
		}

		interfaces.forEach((s, i) -> {
			mergerConfig.getNode().interfaces.add(s);
			if (i.size() == mergerConfig.getInfos().size()) {
				return;
			}

			AnnotationVisitor n = mergerConfig.getNode().visitAnnotation(INTERFACE, true);
			AnnotationVisitor visitor = n.visitArray("platform");
			for (ClassInfo info : i) {
				visitor.visit("platform", info.createPlatformAnnotation());
			}

			n.visit("parent", Type.getObjectType(s));
		});
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		List<String> interfaces = new ArrayList<>(in.interfaces);
		if (in.visibleTypeAnnotations != null) {
			Iterator<TypeAnnotationNode> iterator = in.visibleTypeAnnotations.iterator();
			while (iterator.hasNext()) {
				TypeAnnotationNode annotation = iterator.next();
				if (!SplitterUtil.matches(annotation, available)) {
					TypeReference reference = new TypeReference(annotation.typeRef);
					if (reference.getSort() == TypeReference.CLASS_EXTENDS) {
						int i = reference.getSuperTypeIndex();
						if (i >= 0) {
							iterator.remove();
							in.interfaces.remove(interfaces.get(i));
						}
					}
				}
			}
		}

		Iterator<AnnotationNode> iterator = in.visibleAnnotations.iterator();
		while (iterator.hasNext()) {
			AnnotationNode annotation = iterator.next();
			if (INTERFACE.equals(annotation.desc)) {
				List<Object> values = annotation.values;
				if (!SplitterUtil.matches((List<AnnotationNode>) values.get(values.indexOf("platform") + 1), available)) {
					in.interfaces.remove(values.get(values.indexOf("parent") + 1));
					iterator.remove();
				}
			}
		}
		return false;
	}
}
