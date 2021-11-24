package io.github.astrarre.amalgamation.gradle.dependencies.transform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.github.astrarre.amalgamation.gradle.dependencies.AbstractSelfResolvingDependency;
import net.devtech.zipio.OutputTag;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

public class SingleTransformDependency extends AbstractSelfResolvingDependency {
	public static final Logger LOGGER = Logger.getLogger("Amalg-Transform");
	final Dependency input;
	final TransformDependency<?, ?> dependency;
	final List<OutputTag> outputs = new ArrayList<>();
	boolean initialized;
	String mutableVersion;
	String mutableName;
	String hash;

	public SingleTransformDependency(String group, String name, String version, Dependency input, TransformDependency<?, ?> dependency) {
		super(dependency.project, group, name, version);
		this.mutableVersion = version;
		this.mutableName = name;
		this.input = input;
		this.dependency = dependency;
	}

	public void append(String hash) {
		this.hash = hash;
		this.mutableName += "_" + this.mutableVersion;
		this.mutableVersion = hash;
		this.initialized = true;
	}

	public String jarName(@Nullable String classifier) {
		if(!initialized) {
			LOGGER.warning("TransformDependencies' version not initialized, sources may not attach properly!");
		}
		if(classifier == null) {
			return this.getName() + "-" + this.getVersion() + ".jar";
		} else {
			return this.getName() + "-" + this.getVersion() + "-" + classifier + ".jar";
		}
	}

	public void setVersion(String version) {
		this.mutableVersion = version;
	}

	@Override
	public String getVersion() {
		if(!initialized) {
			LOGGER.warning("TransformDependencies' version not initialized, sources may not attach properly!");
		}
		return this.mutableVersion;
	}

	public void setName(String mutableName) {
		this.mutableName = mutableName;
	}

	@Override
	public String getName() {
		return this.mutableName;
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		this.dependency.resolve();
		return this.outputs
				       .stream()
				       .map(OutputTag::getVirtualPath)
				       .toList();
	}
}
