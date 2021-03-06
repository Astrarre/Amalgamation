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

package io.github.f2bb.amalgamation.platform.merger.impl.field;

import io.github.f2bb.amalgamation.platform.merger.impl.Merger;
import io.github.f2bb.amalgamation.platform.merger.impl.MergerConfig;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import io.github.f2bb.amalgamation.platform.util.SplitterUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.*;

public class FieldMerger implements Merger {

	@Override
	public void merge(MergerConfig mergerConfig) {
		Map<FieldKey, List<ClassInfo>> toMerge = new HashMap<>();
		for (ClassInfo info : mergerConfig.getInfos()) {
			for (FieldNode method : info.node.fields) {
				toMerge.computeIfAbsent(new FieldKey(method), c -> new ArrayList<>()).add(info);
			}
		}

		int[] counter = {0};
		toMerge.forEach((key, info) -> {
			FieldNode clone = new FieldNode(key.node.access, key.node.name, key.node.desc, key.node.signature, null);
			key.node.accept(new ClassVisitor(ASM9) {
				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
					return clone;
				}
			});

			if (this.hasField(mergerConfig.getNode(), key)) {
				if (clone.visibleAnnotations == null) {
					clone.visibleAnnotations = new ArrayList<>();
				}

				clone.visibleAnnotations.add(ClassInfo.displace(clone.name));
				clone.name += "_" + counter[0]++;
			}

			if ((clone.access & (ACC_BRIDGE | ACC_SYNTHETIC)) == 0 && mergerConfig.getInfos().size() != info.size()) {
				if (clone.visibleAnnotations == null) {
					clone.visibleAnnotations = new ArrayList<>();
				}

				for (ClassInfo classInfo : info) {
					clone.visibleAnnotations.add(classInfo.createPlatformAnnotation());
				}
			}

			mergerConfig.getNode().fields.add(clone);
		});
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		in.fields.removeIf(field -> !SplitterUtil.matches(field.visibleAnnotations, available));
		in.fields.forEach(f -> {
			Iterator<AnnotationNode> iterator = f.visibleAnnotations.iterator();
			while (iterator.hasNext()) {
				AnnotationNode annotation = iterator.next();
				if (ClassInfo.DISPLACE.equals(annotation.desc)) {
					f.name = (String) annotation.values.get(1);
					iterator.remove();
				} else if(ClassInfo.PLATFORM.equals(annotation.desc)) {
					iterator.remove();
				}
			}
		});
		return false;
	}

	private boolean hasField(ClassNode node, FieldKey key) {
		for (FieldNode method : node.fields) {
			try {
				if (method.name.equals(key.node.name) && method.desc.equals(key.node.desc)) {
					return true;
				}
			} catch (NullPointerException e) {
				System.out.println(method + " " + method.name + " " + method.desc);
				System.out.println(key.node + " " + key.node.name + " " + key.node.desc);
			}
		}

		return false;
	}
}
