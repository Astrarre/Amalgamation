package net.devtech.testbytecodemerge.annotation;

/**
 * a platform specific interface
 */
public @interface Interface {
	/**
	 * @return this class's super class on the specified platform
	 */
	Class<?> parent();

	/**
	 * @return the platforms the class is a super on
	 */
	Platform[] platforms();
}
