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

package net.devtech.testbytecodemerge.mergers.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.devtech.testbytecodemerge.ClassInfo;
import net.devtech.testbytecodemerge.mergers.Merger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodMerger implements Merger {
	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		Map<MethodKey, List<ClassInfo>> toMerge = new HashMap<>();
		for (ClassInfo info : infos) {
			for (MethodNode method : info.node.methods) {
				toMerge.computeIfAbsent(new MethodKey(method), c -> new ArrayList<>()).add(info);
			}
		}

		int[] counter = {0};
		toMerge.forEach((key, info) -> {
			MethodNode clone = new MethodNode(key.node.access, key.node.name, key.node.desc, key.node.signature, null);
			key.node.accept(clone);
			if (this.hasMethod(node, key)) {
				clone.name += "_" + counter[0]++;
			}

			if ((clone.access & (ACC_BRIDGE | ACC_SYNTHETIC)) == 0 && infos.size() != info.size()) {
				if (clone.visibleAnnotations == null) {
					clone.visibleAnnotations = new ArrayList<>();
				}

				for (ClassInfo classInfo : info) {
					clone.visibleAnnotations.add(classInfo.createPlatformAnnotation());
				}
			}

			node.methods.add(clone);
		});
	}

	private boolean hasMethod(ClassNode node, MethodKey key) {
		for (MethodNode method : node.methods) {
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