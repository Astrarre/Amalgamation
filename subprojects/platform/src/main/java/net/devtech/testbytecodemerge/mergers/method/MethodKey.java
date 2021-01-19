package net.devtech.testbytecodemerge.mergers.method;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MethodKey {
	public final MethodNode node;

	public MethodKey(MethodNode node) {this.node = node;}

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

		return a.access == b.access && a.name.equals(b.name) && a.desc.equals(b.desc) && Objects.equals(a.signature, b.signature) && areInstructionsEqual(a, b);
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
			return aN.type == bN.type && Objects.equals(aN.local, bN.local) && aN.stack.equals(bN.stack);
		} else if (a instanceof LineNumberNode) {
			// kindof irrelavent
			return true;
		}
		throw new UnsupportedOperationException(a.getClass() + " " + a.getType());
	}

	private static boolean compareLabels(LabelNode a, LabelNode b) {
		Label al = a.getLabel(), bl = b.getLabel();
		// todo
		return true;
		//return al.getOffset() == bl.getOffset();
	}
}
