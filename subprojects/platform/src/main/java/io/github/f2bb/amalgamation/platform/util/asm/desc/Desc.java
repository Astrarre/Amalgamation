package io.github.f2bb.amalgamation.platform.util.asm.desc;

import java.util.Objects;

public class Desc {
	public static final int METHOD = 0;
	public static final int FIELD = 1;
	public final String name, desc;
	private final int hashCode, type;

	public Desc(String name, String desc, int type) {
		this.name = name;
		this.desc = desc;
		this.type = type;

		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (desc != null ? desc.hashCode() : 0);
		result = 31 * result + (type);
		this.hashCode = result;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Desc)) {
			return false;
		}

		Desc desc1 = (Desc) object;
		return Objects.equals(this.name, desc1.name) && this.type == desc1.type && Objects.equals(this.desc,
				desc1.desc);

	}

	@Override
	public String toString() {
		if (this.type == METHOD) {
			return this.name + this.desc;
		} else {
			return this.desc + ';' + this.name;
		}
	}
}
