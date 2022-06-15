package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.devtech.filepipeline.impl.util.FPInternal;
import org.gradle.api.tasks.Internal;
import org.gradle.jvm.tasks.Jar;

public abstract class AbstractRemapJarTask<T extends AbstractRemapAllTask<? extends AbstractRemapJarTask<T>>> extends Jar implements RemapTask {
	final List<T> groups = new ArrayList<>();
	boolean isOutdated;

	protected abstract void remap() throws IOException;

	/**
	 * megamind moment
	 */
	@Override
	protected void copy() {
		super.copy();
		try {
			if(!this.groups.isEmpty()) {
				isOutdated = true;
				return;
			}
			this.remap();
		} catch(IOException e) {
			throw FPInternal.rethrow(e);
		}
	}

	@Internal
	protected Path getCurrent() {
		return this.getArchiveFile().get().getAsFile().toPath();
	}
}
