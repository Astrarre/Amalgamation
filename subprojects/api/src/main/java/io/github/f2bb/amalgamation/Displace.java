package io.github.f2bb.amalgamation;

/**
 * if the method with the given name is stripped, the current method will replace it.
 *
 * eg. if you have a fabric-only method called foo, and a forge method called bar {@code @Displace("foo")} will cause `bar` to take the place of `foo` in the forge-specific jar
 */
public @interface Displace {
	/**
	 * @return the name of the method to displace
	 */
	String value();
}
