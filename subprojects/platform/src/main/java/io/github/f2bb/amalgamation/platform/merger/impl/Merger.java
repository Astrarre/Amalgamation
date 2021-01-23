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

import java.util.List;
import java.util.Set;

import io.github.f2bb.amalgamation.platform.merger.impl.field.FieldMerger;
import io.github.f2bb.amalgamation.platform.merger.impl.method.MethodMerger;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public interface Merger extends Opcodes {
	Merger MERGER;
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

	void merge(ClassNode node, List<ClassInfo> infos);

	boolean strip(ClassNode in, Set<String> available);

	default Merger andThen(Merger merger) {
		return new Merger() {
			@Override
			public void merge(ClassNode node, List<ClassInfo> infos) {
				Merger.this.merge(node, infos);
				merger.merge(node, infos);
			}

			@Override
			public boolean strip(ClassNode in, Set<String> available) {
				return Merger.this.strip(in, available) || merger.strip(in, available);
			}
		};
	}
}
