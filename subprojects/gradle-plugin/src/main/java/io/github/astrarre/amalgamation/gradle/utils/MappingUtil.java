package io.github.astrarre.amalgamation.gradle.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.tinyremapper.IMappingProvider;

public class MappingUtil {
	public static void loadMappings(MappingSet mappings, File file, String from, String to) throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(file.toPath(),
				null); BufferedReader reader = Files.newBufferedReader(fileSystem.getPath("/mappings/mappings.tiny"))) {
			new TinyMappingsReader(TinyMappingFactory.loadWithDetection(reader), from, to).read(mappings);
		}
	}

	public static void iterateClasses(MappingSet mappings, Consumer<ClassMapping<?, ?>> consumer) {
		for (TopLevelClassMapping topLevelClassMapping : mappings.getTopLevelClassMappings()) {
			iterateClasses(topLevelClassMapping, consumer);
		}
	}

	public static IMappingProvider createMappingProvider(MappingSet mappings) {
		return out -> iterateClasses(mappings, classMapping -> {
			String owner = classMapping.getFullObfuscatedName();
			out.acceptClass(owner, classMapping.getFullDeobfuscatedName());

			for (MethodMapping methodMapping : classMapping.getMethodMappings()) {
				out.acceptMethod(new IMappingProvider.Member(owner, methodMapping.getObfuscatedName(), methodMapping.getObfuscatedDescriptor()),
						methodMapping.getDeobfuscatedName());
			}

			for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
				fieldMapping.getType()
				            .ifPresent(fieldType -> out.acceptField(new IMappingProvider.Member(owner,
						            fieldMapping.getObfuscatedName(),
						            fieldType.toString()), fieldMapping.getDeobfuscatedName()));
			}
		});
	}

	private static void iterateClasses(ClassMapping<?, ?> classMapping, Consumer<ClassMapping<?, ?>> consumer) {
		consumer.accept(classMapping);

		for (InnerClassMapping innerClassMapping : classMapping.getInnerClassMappings()) {
			iterateClasses(innerClassMapping, consumer);
		}
	}
}
