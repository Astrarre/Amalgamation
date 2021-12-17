package io.github.astrarre.amalgamation.gradle.dependencies.remap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public record MappingTarget(Project project, Dependency forward, String from, String to) {
	public Mappings.Namespaced read() throws IOException {
		return Mappings.read(this.project, this.forward, this.from, this.to);
	}

	public void hash(Hasher hasher) {
		hasher.putString(this.from, StandardCharsets.UTF_8);
		hasher.putString(this.to, StandardCharsets.UTF_8);
		AmalgIO.hashDep(hasher, this.project, this.forward);
	}
}
