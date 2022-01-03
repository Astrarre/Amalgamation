package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.mixin.MixinExtensionReborn;
import io.github.astrarre.amalgamation.gradle.tasks.remap.remap.AwResourceRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemapAllJars extends AbstractRemapAllTask<RemapJar> {
	@Override
	public void remapAll() throws IOException {
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
		List<Mappings.Namespaced> mappings = this.readAllMappings();
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
		Set<Path> classpath = new LinkedHashSet<>();
		for(RemapJar jar : this.remapJars) {
			this.ensureCleanInput(jar);
			if(jar.isOutdated) { // use as classpath
				InputTag input = remapper.createInputTag();
				pairs.add(new RemapJarPair(jar, input));
				// this is intentional, remap jar essentially just overwrites it's output after the fact
				for(File file : jar.getOutputs().getFiles()) {
					futures.add(remapper.readInputsAsync(input, file.toPath()));
				}
			} else {
				for(File file : jar.getInputs().getFiles()) {
					classpath.add(file.toPath().toRealPath());
				}
			}

			for(File file : jar.getClasspath().get()) {
				classpath.add(file.toPath().toRealPath());
			}
		}

		futures.add(remapper.readClassPathAsync(classpath.toArray(Path[]::new)));

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
