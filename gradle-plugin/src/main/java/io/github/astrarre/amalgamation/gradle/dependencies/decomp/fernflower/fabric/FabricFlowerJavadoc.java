package io.github.astrarre.amalgamation.gradle.dependencies.decomp.fernflower.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.dependencies.decomp.DecompilationMetadata;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;

public final class FabricFlowerJavadoc {
	public static void configure(Map<String, Object> options, DecompilationMetadata metadata) {
		if(!metadata.javaDocs().isEmpty()) {
			List<Mappings.Namespaced> mappings = metadata.javaDocs();
			List<IFabricJavadocProvider> providers = new ArrayList<>();
			for(Mappings.Namespaced mapping : mappings) {
				providers.add(new TinyJavadocProvider(mapping));
			}
			options.put(IFabricJavadocProvider.PROPERTY_NAME, CombinedFabricJavadocProvider.combine(providers));
		}
	}
}
