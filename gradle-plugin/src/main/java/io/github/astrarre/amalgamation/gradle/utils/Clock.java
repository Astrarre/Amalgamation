package io.github.astrarre.amalgamation.gradle.utils;


import org.gradle.api.logging.Logger;

public class Clock implements AutoCloseable {
	private final Logger logger;
	private final long start;
	public String message;

	public Clock(String message, Logger logger) {
		this.message = message;
		this.logger = logger;
		this.start = System.currentTimeMillis();
	}

	@Override
	public void close() {
		if (this.logger != null) {
			this.logger.lifecycle(String.format(this.message, System.currentTimeMillis() - this.start));
		}
	}

	/**
	 * @return nothing, because it throws
	 * @throws T rethrows {@code throwable}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> RuntimeException rethrow(Throwable throwable) throws T {
		throw (T) throwable;
	}
}
