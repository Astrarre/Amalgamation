package io.github.astrarre;

import io.github.astrarre.amalgamation.api.Access;
import io.github.astrarre.amalgamation.api.Displace;
import io.github.astrarre.amalgamation.api.Interface;
import io.github.astrarre.amalgamation.api.Parent;
import io.github.astrarre.amalgamation.api.Platform;
import org.objectweb.asm.Type;

public class Classes {
	public static final String PLATFORM_DESC = Type.getDescriptor(Platform.class);
	public static final String INTERFACE_DESC = Type.getDescriptor(Interface.class);
	public static final String ACCESS_DESC = Type.getDescriptor(Access.class);
	public static final String PARENT_DESC = Type.getDescriptor(Parent.class);
	public static final Type OBJECT_TYPE = Type.getType(Object.class);
	public static final String DISPLACE_DESC = Type.getDescriptor(Displace.class);
}
