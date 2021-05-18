package io.github.astrarre.amalgamation.utils;


import org.gradle.api.logging.Logger;

public class Clock implements AutoCloseable {
	public String message;
	private final Logger logger;
	private final long start;

	public Clock(String message, Logger logger) {
		this.message = message;
		this.logger = logger;
		this.start = System.currentTimeMillis();
	}

	@Override
	public void close() {
		this.logger.lifecycle(String.format(this.message, System.currentTimeMillis() - this.start));
	}
}
