package io.github.astrarre.amalgamation.utils;


import org.slf4j.Logger;

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
		this.logger.info(String.format(this.message, System.currentTimeMillis() - this.start));
	}
}
