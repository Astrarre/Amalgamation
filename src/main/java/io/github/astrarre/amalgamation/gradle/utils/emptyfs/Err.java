package io.github.astrarre.amalgamation.gradle.utils.emptyfs;

public class Err {
	/**
	 * @return nothing, because it throws
	 * @throws T rethrows {@code throwable}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> RuntimeException rethrow(Throwable throwable) throws T {
		throw (T) throwable;
	}
}
