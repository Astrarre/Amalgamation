package io.github.astrarre.amalgamation.gradle.utils;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.google.common.collect.ImmutableMap;
import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.EnvironmentAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.PlatformAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.annotationHandler.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.merger.Merger;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.platform.api.classpath.PathsContext;
import io.github.astrarre.amalgamation.gradle.platform.api.impl.ClassTypeSolver;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.AccessMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.ClassMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.HeaderMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.InnerClassAttributeMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.InterfaceMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.SignatureMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.SuperclassMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.field.FieldMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.method.MethodMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class MergeUtil {
	/**
	 * if a file in the root directory of a jar has the following name, the entire jar contains nothing but class files
	 */
	public static final String RESOURCES_MARKER_FILE = "resourceJar.marker";
	/**
	 * if the first entry of a zip file is a file with the name of this field, it is configured
	 */
	public static final String MERGER_META_FILE = "merger_metadata.properties";
	// start merger meta properties
	public static final String RESOURCES = "resources";
	public static final String PLATFORMS = "platforms";
	public static final Map<String, ?> CREATE_ZIP = ImmutableMap.of("create", "true");

	public static final List<AnnotationHandler> ONLY_PLATFORM = Collections.singletonList(PlatformAnnotationHandler.INSTANCE);
	public static List<AnnotationHandler> defaultHandlers() {
		List<AnnotationHandler> handlers = new ArrayList<>();
		handlers.add(EnvironmentAnnotationHandler.INSTANCE);
		handlers.add(PlatformAnnotationHandler.INSTANCE);
		return handlers;
	}
	public static List<Merger> defaults(Map<String, ?> config) {
		List<Merger> mergers = new ArrayList<>(); // order matters sometimes
		mergers.add(new AccessMerger(config));
		mergers.add(new ClassMerger(config));
		mergers.add(new HeaderMerger(config));
		mergers.add(new InnerClassAttributeMerger(config));
		mergers.add(new InterfaceMerger(config));
		mergers.add(new SuperclassMerger(config));
		mergers.add(new SignatureMerger(config));
		mergers.add(new MethodMerger(config));
		mergers.add(new FieldMerger(config));
		return mergers;
	}

	public static void merge(List<AnnotationHandler> handlers, Map<String, List<String>> compact, List<Merger> mergers, Path dest, Map<List<String>, Iterable<File>> toMerge, Function<List<String>, Path> resources, boolean shouldLeaveMarker)
			throws IOException, URISyntaxException {
		Map<List<String>, PathsContext> contexts = new HashMap<>();
		Set<String> fileNames = new HashSet<>();

		List<Closeable> toClose = new ArrayList<>();
		try {
			for (Map.Entry<List<String>, Iterable<File>> entry : toMerge.entrySet()) {
				JavaParser parser = new JavaParser();
				List<Path> paths = new ArrayList<>();
				for (File file : entry.getValue()) {
					FileSystem system = FileSystems.newFileSystem(file.toPath(), null);
					for (Path directory : system.getRootDirectories()) {
						paths.add(directory);
					}
					toClose.add(system);
				}

				CombinedTypeSolver solver = new CombinedTypeSolver();
				solver.add(new ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader()));
				solver.add(new ClassTypeSolver(paths, fileNames));
				parser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));
				contexts.put(entry.getKey(), new PathsContext(parser, paths));
			}

			Map<List<String>, ZipOutputStream> resourceOutput = new HashMap<>();
			ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(dest)));
			if (shouldLeaveMarker) {
				output.putNextEntry(new ZipEntry(MERGER_META_FILE));
				output.closeEntry();
			}
			toClose.add(output);
			for (String name : fileNames) {
				if (RESOURCES_MARKER_FILE.equals(name) || MERGER_META_FILE.equals(name))
					continue;
				List<RawPlatformClass> classes = new ArrayList<>();
				for (Map.Entry<List<String>, PathsContext> entry : contexts.entrySet()) {
					List<String> strings = entry.getKey();
					PathsContext context = entry.getValue();
					byte[] data = context.getResourceAsByteArray(name);
					if (data != null && name.endsWith(".class")) {
						ClassReader reader = new ClassReader(data);
						ClassNode node = new ClassNode();
						reader.accept(node, 0);
						classes.add(new RawPlatformClass(new PlatformId(strings), node, context));
					} else if (data != null) {
						ZipOutputStream dump = resourceOutput.computeIfAbsent(strings, strings1 -> {
							try {
								Path pth = resources.apply(strings1);
								Files.createDirectories(pth.getParent());
								ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(pth)));
								toClose.add(zos);
								zos.putNextEntry(new ZipEntry(MERGER_META_FILE));
								Properties properties = new Properties();
								properties.put("platforms", String.join(",", strings));
								properties.put("resources", "true");
								properties.store(zos, "This is a marker file for optimization purposes");
								return zos;
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						});
						dump.putNextEntry(new ZipEntry(name));
						dump.write(data);
						dump.closeEntry();
					}
				}

				if (!classes.isEmpty()) {
					ClassNode merged = new ClassNode();
					for (Merger merger : mergers) {
						merger.merge(classes, merged, compact, handlers);
					}
					ClassWriter writer = new ClassWriter(0);
					merged.accept(writer);
					output.putNextEntry(new ZipEntry(name));
					output.write(writer.toByteArray());
					output.closeEntry();
				}
			}
		} finally {
			for (Closeable closeable : toClose) {
				closeable.close();
			}
		}
	}

	public static boolean matches(List<AnnotationNode> nodes, PlatformId id, List<AnnotationHandler> handlers) {
		boolean visited = false;
		for (PlatformId platform : Platformed.getPlatforms(handlers, nodes, PlatformId.EMPTY)) {
			if(platform == PlatformId.EMPTY) continue;
			visited = true;
			if(platform.names.containsAll(id.names)) {
				return true;
			}
		}
		return !visited;
	}

	public static List<AnnotationNode> stripAnnotations(List<AnnotationNode> strip, PlatformId id, List<AnnotationHandler> handler) {
		List<AnnotationNode> nodes = new ArrayList<>();
		outer:
		for (AnnotationNode node : strip) {
			for (AnnotationHandler annotation : handler) {
				List<String> platforms = annotation.expand(node);
				if(platforms != null) {
					PlatformId platform = new PlatformId(platforms);
					if(platform.names.containsAll(id.names)) {
						List<String> stripped = new ArrayList<>(platform.names);
						stripped.removeAll(id.names);
						if(!stripped.isEmpty()) {
							PlatformId created = new PlatformId(stripped);
							nodes.add(created.createAnnotation(handler));
						}
					}
					continue outer;
				}
			}
			nodes.add(node);
		}
		return nodes;
	}

	public static AnnotationNode withDesc(List<AnnotationNode> nodes, String desc, Predicate<AnnotationNode> predicate) {
		if(nodes == null) return null;
		for (AnnotationNode node : nodes) {
			if(desc.equals(node.desc) && predicate.test(node)) {
				return node;
			}
		}
		return null;
	}

	public static <T> T get(AnnotationNode node, String id, T def) {
		if(node == null) return def;
		int index = node.values.indexOf(id);
		if(index == -1) return def;
		return (T) node.values.get(index + 1);
	}

	public static boolean is(AnnotationNode node, String id, Object val) {
		if(node == null) return false;
		int index = node.values.indexOf(id);
		if(index == -1) return false;
		return val.equals(node.values.get(index + 1));
	}

}
