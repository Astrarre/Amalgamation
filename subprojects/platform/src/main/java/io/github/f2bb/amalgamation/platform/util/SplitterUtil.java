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

import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Set;

public class SplitterUtil {
	public static boolean matches(AnnotationNode node, Set<String> platforms) {
		if (ClassInfo.PLATFORM.equals(node.desc)) {
			return platforms.containsAll((List<String>) node.values.get(1));
		} else {
			return false;
		}
	}

	public static boolean matches(List<AnnotationNode> nodes, Set<String> platforms) {
		boolean notVisited = true;
		if (nodes == null)
			return true;
		for (AnnotationNode node : nodes) {
			if (matches(node, platforms)) {
				return true;
			}

			if(notVisited && ClassInfo.PLATFORM.equals(node.desc)) {
				notVisited = false;
			}
		}
		return notVisited;
	}

}
