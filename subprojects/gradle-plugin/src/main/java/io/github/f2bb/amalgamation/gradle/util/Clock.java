package io.github.f2bb.amalgamation.gradle.util;

import org.gradle.api.logging.Logger;

public class Clock {
	private final String message;
	private final Logger logger;
	private final long start;

	public Clock(String message, Logger logger) {
		this.message = message;
		this.logger = logger;
		this.start = System.currentTimeMillis();
	}

	public void end() {
		this.logger.lifecycle(String.format(this.message, System.currentTimeMillis() - this.start));
	}
}
