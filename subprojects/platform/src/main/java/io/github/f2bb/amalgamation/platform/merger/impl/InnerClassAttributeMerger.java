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
import org.objectweb.asm.tree.InnerClassNode;

import java.util.List;
import java.util.Set;

class InnerClassAttributeMerger implements Merger {

	@Override
	public void merge(MergerConfig mergerConfig, ClassNode merged, List<ClassInfo> components) {
		components.stream()
		            .map(ClassInfo::getNode)
		            .map(c -> c.innerClasses)
		            .flatMap(List::stream)
		            .map(io.github.astrarre.merger.impl.InnerClassAttributeMerger.InnerClassNodeWrapper::new)
		            .distinct()
		            .map(io.github.astrarre.merger.impl.InnerClassAttributeMerger.InnerClassNodeWrapper::getNode)
		            .forEach(n -> merged.innerClasses.add(n));
	}

	@Override
	public boolean strip(ClassNode in, Set<String> available) { // todo strip invalid classes, need access to classpath
		return false;
	}


}
