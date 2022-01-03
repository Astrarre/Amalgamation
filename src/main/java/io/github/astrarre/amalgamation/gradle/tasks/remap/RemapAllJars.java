package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.mixin.MixinExtensionReborn;
import io.github.astrarre.amalgamation.gradle.tasks.remap.remap.AwResourceRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.tasks.properties.PropertyVisitor;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemapAllJars extends DefaultTask {
	final List<RemapJar> remapJars = new ArrayList<>();

	public RemapAllJars() {
		// this should be false for reasons
		this.getOutputs().cacheIf(t -> false);
	}

	public void addTask(RemapJar jar) {
		jar.finalizedBy(this);
		this.dependsOn(jar);
		this.remapJars.add(jar);
		jar.groups.add(this);
	}

	@TaskAction
	public void doRemapAll() {
		try {
			this.remapAll();
		} catch(Throwable t) {
			this.getLogger().error("Detected error in RemapAll task, deleting all outputs to ensure cache validity");
			List<IOException> exceptions = new ArrayList<>();
			for(RemapJar jar : this.remapJars) {
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
			throw U.rethrow(t);
		}
	}


	public void remapAll() throws IOException {
		boolean hasJob = false;
		for(RemapJar jar : remapJars) {
			if(jar.isOutdated) {
				hasJob = true;
			}
		}
		if(!hasJob) {
			return;
		}

		// validate common state
		boolean isAccessWidenerEnabled = this.only(
				RemapJar::getIsAccessWidenerRemappingEnabled,
				"Different isAccessWidenerRemappingEnabled states, all RemapJar states must be the same within a remapAll group!"
		);
		String destinationNamespace = this.only(
				RemapJar::getAccessWidenerDestinationNamespace,
				"Multiple different accessWidener destination namespace states, all RemapJar states must be the same within a remapAll group!"
		);

		boolean enableExperimentalMixinRemapper = this.only(
				RemapJar::getUseExperimentalMixinRemapper,
				"Different useExperimentalMixinRemapper states, all RemapJar states must be the same within a remapAll group!"
		);

		TinyRemapper.Builder builder = TinyRemapper.newRemapper();

		// read mappings
		record MappingEntry(File file, String from, String to) {}
		Set<MappingEntry> entries = new LinkedHashSet<>();
		for(RemapJar jar : remapJars) {
			for(RemapTask.MappingEntry entry : jar.getMappings().get()) {
				entries.add(new MappingEntry(
						entry.mappings,
						entry.from,
						entry.to
				));
			}
		}
		List<Mappings.Namespaced> mappings = new ArrayList<>();
		for(MappingEntry entry : entries) {
			mappings.add(Mappings.from(entry.file.toPath(), entry.from, entry.to));
		}
		builder.withMappings(Mappings.from(mappings));

		List<OutputConsumerPath.ResourceRemapper> remappers = new ArrayList<>();
		if(enableExperimentalMixinRemapper) {
			builder.extension(new MixinExtensionReborn(this.getLogger()));
			remappers.add(new RemapJar.MixinConfigRefmapAppender());
		}

		TinyRemapper remapper = builder.build();

		// populate inputs
		record RemapJarPair(RemapJar jar, InputTag tag) {}
		List<RemapJarPair> pairs = new ArrayList<>();
		List<CompletableFuture<?>> futures = new ArrayList<>();
		for(RemapJar jar : this.remapJars) {
			FileCollection inputs = jar.getInputs().getFiles();
			FileCollection outputs = jar.getOutputs().getFiles();
			for(File file : outputs) {
				if(inputs.contains(file)) {
					throw new UnsupportedOperationException(jar + " output overwrites input file! " + file);
				}
			}

			if(jar.isOutdated) { // use as classpath
				InputTag input = remapper.createInputTag();
				pairs.add(new RemapJarPair(jar, input));
				// this is intentional, remap jar essentially just overwrites it's output after the fact
				for(File file : jar.getOutputs().getFiles()) {
					futures.add(remapper.readInputsAsync(input, file.toPath()));
				}
			} else {
				for(File file : jar.getInputs().getFiles()) {
					futures.add(remapper.readClassPathAsync(file.toPath()));
				}
			}

			for(File file : jar.getClasspath().get()) {
				futures.add(remapper.readClassPathAsync(file.toPath()));
			}
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		if(isAccessWidenerEnabled) {
			String namespace = destinationNamespace;
			if(namespace.equals("autodetect")) {
				namespace = Iterables.getOnlyElement(mappings.stream().map(Mappings.Namespaced::to).distinct().toList());
			}
			String finalNamespace = namespace;
			remappers.add(new AwResourceRemapper(() -> finalNamespace));
		}

		for(RemapJarPair pair : pairs) {
			Path file = pair.jar.getOutputs().getFiles().getSingleFile().toPath();
			try(OutputConsumerPath ocp = new OutputConsumerPath.Builder(file).build()) {
				if(enableExperimentalMixinRemapper) {
					RemapJar.addEmptyRefmap(ocp);
				}
				ocp.addNonClassFiles(file, remapper, remappers);
				remapper.apply(ocp, pair.tag);
			} finally {
				remapper.finish();
			}
		}
	}

	<T> T only(Function<RemapJar, Property<T>> stream, String message) {
		List<T> ts = this.remapJars.stream().map(stream).map(Provider::get).distinct().toList();
		if(ts.size() > 1) {
			throw new IllegalArgumentException(String.format(message, ts));
		}
		return ts.get(0);
	}
}
