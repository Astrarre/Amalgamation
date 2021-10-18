package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.dependencies.MappingTarget;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Lazy;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.cadixdev.mercury.remapper.MercuryRemapper;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;

public interface RemapTask extends Task {
	class MappingEntry {
		public String from, to;
		public File mappings;

		@Input
		public String getFrom() {
			return this.from;
		}

		@Input
		public String getTo() {
			return this.to;
		}

		@InputFile
		public File getMappings() {
			return this.mappings;
		}
	}

	@InputFiles
	Property<FileCollection> getClasspath();

	@Nested
	@Input
	ListProperty<MappingEntry> getMappings();

	default void mappings(Object dep, String from, String to) {
		var lazy = Lazy.of(() -> {
			MappingEntry entry = new MappingEntry();
			entry.from = from;
			entry.to = to;
			entry.mappings = AmalgIO.resolve(this.getProject(), dep);
			return entry;
		});
		this.getMappings().add(this.getProject().provider(lazy));
	}

	default void mappings(MappingTarget target) {
		this.mappings(target.forward(), target.from(), target.to());
	}

	default List<Mappings.Namespaced> readMappings() throws IOException {
		List<Mappings.Namespaced> namespaced = new ArrayList<>();
		for(var entry : this.getMappings().get()) {
			namespaced.add(Mappings.from(entry.mappings.toPath(), entry.from, entry.to));
		}
		return namespaced;
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
