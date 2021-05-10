package io.github.astrarre.merger;

import io.github.f2bb.amalgamation.Access;
import io.github.f2bb.amalgamation.Displace;
import io.github.f2bb.amalgamation.Interface;
import io.github.f2bb.amalgamation.Parent;
import io.github.f2bb.amalgamation.Platform;
import org.objectweb.asm.Type;

public class Classes {
	public static final String PLATFORM_DESC = Type.getDescriptor(Platform.class);
	public static final String INTERFACE_DESC = Type.getDescriptor(Interface.class);
	public static final String ACCESS_DESC = Type.getDescriptor(Access.class);
	public static final String PARENT_DESC = Type.getDescriptor(Parent.class);
	public static final Type OBJECT_TYPE = Type.getType(Object.class);
	public static final String DISPLACE_DESC = Type.getDescriptor(Displace.class);
}
