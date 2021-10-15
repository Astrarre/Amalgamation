package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.dependencies.MappingTarget;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.cadixdev.mercury.remapper.MercuryRemapper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;

public interface RemapTask extends Task {
	@Input
	Property<String> getFrom();

	@Input
	Property<String> getTo();

	@InputFiles
	Property<FileCollection> getClasspath();

	@InputFile
	Property<File> getMappings();

	default void mappings(Object dep, String from, String to) {
		this.getFrom().set(from);
		this.getTo().set(to);
		this.getMappings().set(AmalgIO.resolve(this.getProject(), dep));
	}

	default void mappings(MappingTarget target) {
		this.mappings(target.forward(), target.from(), target.to());
	}

	static Mercury createMercury(MappingSet set, Property<FileCollection> collection) throws IOException {
		Mercury mercury = new Mercury();
		FileCollection classpath = collection.get();
		var remapper = MercuryRemapper.create(set);

		mercury.getProcessors().add(remapper);

		for(File file : classpath) {
			mercury.getClassPath().add(file.toPath());
		}

		return mercury;
	}
}
