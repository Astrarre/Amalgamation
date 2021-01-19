package io.github.f2bb.amalgamation;

/**
 * a platform-specific super class
 */
public @interface Parent {
	/**
	 * @return this class's super class on the specified platform
	 */
	Class<?> parent();

	/**
	 * @return the platforms the class is a super on
	 */
	Platform[] platforms();
}
