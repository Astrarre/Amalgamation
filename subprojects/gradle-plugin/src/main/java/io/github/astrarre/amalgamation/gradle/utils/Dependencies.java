package io.github.astrarre.amalgamation.gradle.utils;

import java.io.File;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.artifacts.Configuration;

public class Dependencies {
	private transient final Configuration configuration;
	private Set<File> files;

	public Dependencies(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Dependencies)) {
			return false;
		}

		Dependencies key = (Dependencies) object;
		return Objects.equals(this.resolve(), key.resolve());
	}

	private Set<File> resolve() {
		if(this.files == null) {
			this.files = this.configuration.resolve();
		}
		return this.files;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.resolve());
	}
}
