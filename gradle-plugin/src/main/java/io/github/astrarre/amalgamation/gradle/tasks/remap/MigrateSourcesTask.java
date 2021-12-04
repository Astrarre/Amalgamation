package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.nio.file.Path;

import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.Mercury;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public abstract class MigrateSourcesTask extends DefaultTask {
	@InputDirectory
	public abstract Property<String> getInputDir();

	@OutputDirectory
	public abstract Property<String> getOutputDir();

	@InputFile
	public abstract Property<File> getSrcMappings();

	@InputFile
	public abstract Property<File> getDstMappings();

	/**
	 * the namespace in the src mappings that the source is currently in
	 */
	@Input
	public abstract Property<String> getSrcName();

	/**
	 * the namespace in the src mappings that amalg should use to bridge to the next intermediate
	 */
	@Input
	public abstract Property<String> getSrcIntermediateName();

	/**
	 * the intermediary namespace amalg should use to bridge with the src intermediary name
	 */
	@Input
	public abstract Property<String> getDstIntermediateName();

	/**
	 * the namespace the output source should be in
	 */
	@Input
	public abstract Property<String> getDstName();

	@InputFiles
	public abstract Property<FileCollection> getClasspath();

	public void mappings(Object from, Object to, String fromName, String fromIntermediary, String toName, String toIntermediary) {
		this.getSrcMappings().set(AmalgIO.resolveFile(this.getProject(), from));
		this.getDstMappings().set(AmalgIO.resolveFile(this.getProject(), to));
		this.getSrcName().set(fromName);
		this.getSrcIntermediateName().set(fromIntermediary);
		this.getDstName().set(toName);
		this.getDstIntermediateName().set(toIntermediary);
	}

	public void mappings(Object mappings, String fromName, String fromIntermediary, String toName, String toIntermediary) {
		this.mappings(mappings, mappings, fromName, fromIntermediary, toName, toIntermediary);
	}

	public void mappings(Object mappings, String fromName, String intermediary, String toName) {
		this.mappings(mappings, fromName, intermediary, intermediary, toName);
	}

	public void mappings(Object from, Object to) {
		this.mappings(from, to, "named", "intermediary", "intermediary", "named");
	}
	
	static Path from(Property<File> collect) {
		return collect.get().toPath();
	}

	@TaskAction
	public void run() throws Exception {
		var src = Mappings.toLorenz(from(this.getSrcMappings()), this.getSrcName().get(), this.getSrcIntermediateName().get());
		var dst = Mappings.toLorenz(from(this.getDstMappings()), this.getDstName().get(), this.getDstIntermediateName().get());
		MappingSet combined = src.merge(dst);
		Mercury mercury = RemapTask.createMercury(combined, this.getClasspath());
		mercury.rewrite(Path.of(this.getInputDir().get()), Path.of(this.getOutputDir().get()));
	}
}
