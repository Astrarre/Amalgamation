package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.IOException;

import javax.annotation.Nullable;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public record NamespacedMappingsDependency(Project project, Dependency forward, String from, String to) implements Dependency {
	@Override
	@Nullable
	public String getGroup() {
		return forward.getGroup();
	}

	@Override
	public String getName() {
		return forward.getName();
	}

	@Override
	@Nullable
	public String getVersion() {
		return forward.getVersion();
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		return dependency instanceof NamespacedMappingsDependency n && forward.contentEquals(n.forward);
	}

	@Override
	public Dependency copy() {
		return new NamespacedMappingsDependency(project, this.forward, from, to);
	}

	@Override
	@Nullable
	public String getReason() {
		return forward.getReason();
	}

	@Override
	public void because(@Nullable String reason) {
		forward.because(reason);
	}

	public Mappings.Namespaced read() throws IOException {
		return Mappings.read(this.project, this.forward, this.from, this.to);
	}
}
