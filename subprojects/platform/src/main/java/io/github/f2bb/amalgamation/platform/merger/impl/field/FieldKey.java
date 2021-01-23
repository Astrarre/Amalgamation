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

import org.objectweb.asm.tree.FieldNode;

import java.util.Objects;

public class FieldKey {
	public final FieldNode node;

	public FieldKey(FieldNode node) {
		this.node = node;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof FieldKey)) {
			return false;
		}

		FieldKey key = (FieldKey) object;
		FieldNode a = this.node, b = key.node;

		return a.access == b.access && a.name.equals(b.name) && a.desc.equals(b.desc) && Objects.equals(a.signature, b.signature);
	}
}
