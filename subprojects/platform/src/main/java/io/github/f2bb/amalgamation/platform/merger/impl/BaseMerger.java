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

import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Set;

class BaseMerger implements Merger {

	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		node.name = infos.get(0).node.name;

		for (ClassInfo info : infos) {
			node.version = Math.max(node.version, info.node.version);
		}
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) {
		return false;
	}
}
