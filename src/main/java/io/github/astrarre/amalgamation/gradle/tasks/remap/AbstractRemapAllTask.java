package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.filepipeline.impl.util.FPInternal;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractRemapAllTask<T extends AbstractRemapJarTask<? extends AbstractRemapAllTask<T>>> extends DefaultTask {
	final List<T> remapJars = new ArrayList<>();

	public AbstractRemapAllTask() {
		this.getOutputs().cacheIf(t -> false);
	}

	public void addTask(T jar) {
		jar.finalizedBy(this);
		this.dependsOn(jar);
		this.remapJars.add(jar);
		((List)jar.groups).add(this);
	}

	public abstract void remapAll() throws IOException;

	@TaskAction
	public void doRemapAll() {
		try {
			boolean hasJob = false;
			for(T jar : this.remapJars) {
				if(jar.isOutdated) {
					hasJob = true;
				}
			}
			if(!hasJob) {
				return;
			}
			this.remapAll();
		} catch(Throwable t) {
			this.getLogger().error("Detected error in RemapAll task, deleting all outputs to ensure cache validity");
			List<IOException> exceptions = new ArrayList<>();
			for(T jar : this.remapJars) {
				if(jar.isOutdated) {
					for(File file : jar.getOutputs().getFiles()) {
						try {
							Files.delete(file.toPath());
						} catch(IOException e) {
							exceptions.add(e);
						}
					}
				}
			}
			for(IOException exception : exceptions) {
				exception.printStackTrace();
			}
			throw FPInternal.rethrow(t);
		}
	}

	protected void ensureCleanInput(T jar) {
		FileCollection inputs = jar.getInputs().getFiles();
		FileCollection outputs = jar.getOutputs().getFiles();
		for(File file : outputs) {
			if(inputs.contains(file)) {
				throw new UnsupportedOperationException(jar + " output overwrites input file! " + file);
			}
		}
	}

	@NotNull
	protected List<Mappings.Namespaced> readAllMappings() throws IOException {
		record MappingEntry(File file, String from, String to) {}
		Set<MappingEntry> entries = new LinkedHashSet<>();
		for(T jar : this.remapJars) {
			for(RemapTask.MappingEntry entry : jar.getMappings().get()) {
				entries.add(new MappingEntry(
						entry.mappings.getAbsoluteFile(),
						entry.from,
						entry.to
				));
			}
		}
		List<Mappings.Namespaced> mappings = new ArrayList<>();
		for(MappingEntry entry : entries) {
			mappings.add(Mappings.from(entry.file.toPath(), entry.from, entry.to));
		}
		return mappings;
	}

}
