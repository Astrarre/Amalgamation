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

package io.github.astrarre.amalgamation.gradle.merger.impl.method;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;

import java.util.Objects;

public class MethodKey {
	public final boolean compareInstructions;
	public final MethodNode node;

	public MethodKey(boolean instructions, MethodNode node) {
		this.compareInstructions = instructions;
		this.node = node;
	}

	public static boolean areInstructionsEqual(MethodNode a, MethodNode b) {
		InsnList aList = a.instructions, bList = b.instructions;
		if (aList.size() == bList.size()) {
			for (int i = 0; i < aList.size(); i++) {
				if (!isEqual(aList.get(i), bList.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * todo use less hacky solution (giant if statement)
	 *
	 * @see JumpInsnNode
	 * @see InvokeDynamicInsnNode
	 */
	public static boolean isEqual(AbstractInsnNode a, AbstractInsnNode b) {
		if (a.getOpcode() != b.getOpcode()) {
			return false;
		} else if (a.getClass() != b.getClass()) {
			return false;
		} else if (a instanceof InsnNode) {
			return true;
		} else if (a instanceof IntInsnNode) {
			return ((IntInsnNode) a).operand == ((IntInsnNode) b).operand;
		} else if (a instanceof VarInsnNode) {
			return ((VarInsnNode) a).var == ((VarInsnNode) b).var;
		} else if (a instanceof TypeInsnNode) {
			return ((TypeInsnNode) a).desc.equals(((TypeInsnNode) b).desc);
		} else if (a instanceof FieldInsnNode) {
			FieldInsnNode aN = (FieldInsnNode) a, bN = (FieldInsnNode) b;
			return aN.owner.equals(bN.owner) && aN.desc.equals(bN.desc) && aN.name.equals(bN.name);
		} else if (a instanceof MethodInsnNode) {
			MethodInsnNode aN = (MethodInsnNode) a, bN = (MethodInsnNode) b;
			return aN.owner.equals(bN.owner) && aN.desc.equals(bN.desc) && aN.name.equals(bN.name);
		} else if (a instanceof InvokeDynamicInsnNode) {
			InvokeDynamicInsnNode aN = (InvokeDynamicInsnNode) a, bN = (InvokeDynamicInsnNode) b;
			return aN.desc.equals(bN.desc) && aN.name.equals(bN.name) && aN.bsm.equals(bN.bsm) && Objects.deepEquals(aN.bsmArgs, bN.bsmArgs);
		} else if (a instanceof JumpInsnNode) {
			JumpInsnNode aN = (JumpInsnNode) a, bN = (JumpInsnNode) b;
			return compareLabels(aN.label, bN.label);
		} else if (a instanceof LabelNode) {
			return compareLabels((LabelNode) a, (LabelNode) b);
		} else if (a instanceof LdcInsnNode) {
			return ((LdcInsnNode) a).cst.equals(((LdcInsnNode) b).cst);
		} else if (a instanceof IincInsnNode) {
			IincInsnNode aN = (IincInsnNode) a, bN = (IincInsnNode) b;
			return aN.incr == bN.incr && aN.var == bN.var;
		} else if (a instanceof LookupSwitchInsnNode) {
			LookupSwitchInsnNode aN = (LookupSwitchInsnNode) a, bN = (LookupSwitchInsnNode) b;
			if (aN.labels.size() != bN.labels.size()) {
				return false;
			}

			// todo proper label comparison

			return compareLabels(aN.dflt, bN.dflt) && aN.keys.equals(bN.keys);
		} else if (a instanceof MultiANewArrayInsnNode) {
			MultiANewArrayInsnNode aN = (MultiANewArrayInsnNode) a, bN = (MultiANewArrayInsnNode) b;
			return aN.dims == bN.dims && aN.desc.equals(bN.desc);
		} else if (a instanceof FrameNode) {
			FrameNode aN = (FrameNode) a, bN = (FrameNode) b;
			return aN.type == bN.type && Objects.equals(aN.local, bN.local) && Objects.equals(aN.stack, bN.stack);
		} else if (a instanceof LineNumberNode) {
			// kindof irrelavent
			return true;
		} else if (a instanceof TableSwitchInsnNode) {
			TableSwitchInsnNode at = (TableSwitchInsnNode) a, bt = (TableSwitchInsnNode) b;
			return compareLabels(at.dflt, bt.dflt) && at.labels.size() == bt.labels.size() && at.max == bt.max && at.min == bt.min;
		}
		throw new UnsupportedOperationException(a.getClass() + " " + a.getType());
	}

	private static boolean compareLabels(LabelNode a, LabelNode b) {
		Label al = a.getLabel(), bl = b.getLabel();
		// todo
		return true;
		//return al.getOffset() == bl.getOffset();
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
		if (!(object instanceof MethodKey)) {
			return false;
		}

		MethodKey key = (MethodKey) object;
		MethodNode a = this.node, b = key.node;

		return a.access == b.access && a.name.equals(b.name) && a.desc.equals(b.desc) && Objects.equals(a.signature, b.signature) && (!compareInstructions || areInstructionsEqual(a, b));
	}
}
