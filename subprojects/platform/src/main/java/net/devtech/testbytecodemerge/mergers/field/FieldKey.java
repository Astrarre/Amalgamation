package net.devtech.testbytecodemerge.mergers.field;

import java.util.Objects;

import org.objectweb.asm.tree.FieldNode;

public class FieldKey {
	public final FieldNode node;

	public FieldKey(FieldNode node) {this.node = node;}

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
