package io.github.astrarre.amalgamation.gradle.dependencies.transform.aw;

import io.github.astrarre.amalgamation.gradle.dependencies.transform.TransformConfiguration;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;

public interface AccessWidenerHelper extends TransformConfiguration {
	AccessWidenerInput INPUT = new AccessWidenerInput();
	AccessWidenerFile FILE = new AccessWidenerFile();

	final class AccessWidenerInput implements InputType<Void> { // todo this should be Dependency
		AccessWidenerInput() {}
		@Override
		public CacheLocation getCacheLocation() {
			return CacheLocation.PROJECT;
		}
	}

	final class AccessWidenerFile implements InputType<Void> {
		AccessWidenerFile() {}
		@Override
		public CacheLocation getCacheLocation() {
			return CacheLocation.NONE;
		}
	}


	default AccessWidenerInput input() {
		return INPUT;
	}

	default AccessWidenerFile aw() {
		return FILE;
	}
}
