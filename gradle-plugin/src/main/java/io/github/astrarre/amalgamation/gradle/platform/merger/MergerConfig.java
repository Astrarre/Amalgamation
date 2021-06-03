package io.github.astrarre.amalgamation.gradle.platform.merger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;
import io.github.astrarre.amalgamation.gradle.platform.api.annotation.AnnotationHandler;
import io.github.astrarre.amalgamation.gradle.platform.api.classes.RawPlatformClass;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.AccessMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.ClassMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.HeaderMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.InnerClassAttributeMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.InterfaceMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.SignatureMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.SuperclassMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.field.FieldMerger;
import io.github.astrarre.amalgamation.gradle.platform.merger.impl.method.MethodMerger;
import io.github.astrarre.amalgamation.gradle.utils.AmalgamationIO;
import io.github.astrarre.amalgamation.gradle.utils.func.UnsafeIterable;
import io.github.astrarre.amalgamation.gradle.utils.javaparser.PathTypeSolver;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public class MergerConfig {
	protected final Map<Set<String>, List<MergerFile>> toMerge = new HashMap<>(), resources = new HashMap<>();
	protected final List<Merger> mergers = new ArrayList<>();
	protected AnnotationHandler handler; // todo default to @Platform only

	/**
	 * version->1.15.2,1.16.4,etc. platform->fabric,forge,spigot,sponge,etc.
	 */
	protected Map<String, List<String>> combinationsData;

	public MergerConfig addInput(Set<String> platform, MergerFile file) {
		this.toMerge.computeIfAbsent(platform, strings -> new ArrayList<>()).add(file);
		return this;
	}

	public MergerConfig addResource(Set<String> platform, MergerFile file) {
		this.resources.computeIfAbsent(platform, strings -> new ArrayList<>()).add(file);
		return this;
	}

	public MergerConfig combinationsData(Map<String, List<String>> combinationsData) {
		this.combinationsData = combinationsData;
		return this;
	}

	public MergerConfig addAllMergers(List<Merger> mergers) {
		this.mergers.addAll(mergers);
		return this;
	}

	public MergerConfig addMerger(Merger merger) {
		this.mergers.add(merger);
		return this;
	}

	public MergerConfig setAnnotationHandler(AnnotationHandler handler) {
		this.handler = handler;
		return this;
	}

	public void merge(Path mainOutput, boolean sourceMerge, boolean closeMergerFiles) {
		try (FileSystem outputSystem = FileSystems.newFileSystem(new URI("jar:" + mainOutput.toUri()), AmalgamationIO.CREATE_ZIP)) {
			for (Map.Entry<Set<String>, List<MergerFile>> e : this.resources.entrySet()) {
				Set<String> platform = e.getKey();
				List<MergerFile> files = e.getValue();
				for (MergerFile file : files) {
					Properties properties = new Properties();
					for (Path path : file.input) {
						Files.copy(path, file.resourcesOutput);
						Path config = path.resolve(AmalgamationIO.MERGER_META_FILE);
						if(Files.exists(config)) {
							try(Reader reader = Files.newBufferedReader(config)) {
								properties.load(reader);
							}
						}
					}

					String active = properties.getProperty(AmalgamationIO.PLATFORMS);
					properties.setProperty(AmalgamationIO.TYPE, "resources");
					if(active == null) {
						properties.setProperty(AmalgamationIO.PLATFORMS, String.join(",", platform));
					} else {
						Set<String> copyNew = new HashSet<>(platform);
						copyNew.addAll(Arrays.asList(active.split(",")));
						properties.setProperty(AmalgamationIO.PLATFORMS, String.join(",", copyNew));
					}

					try(Writer writer = Files.newBufferedWriter(file.resourcesOutput.resolve(AmalgamationIO.MERGER_META_FILE))) {
						properties.store(writer, "Amalgamation Merger Metadata File");
					}
				}
			}

			Multimap<String, Platformed<Path>> classes = HashMultimap.create(), java = sourceMerge ? HashMultimap.create() : null;
			Map<Set<String>, CombinedTypeSolver> typeSolver = sourceMerge ? new HashMap<>() : null;

			for (Map.Entry<Set<String>, List<MergerFile>> entry : this.toMerge.entrySet()) {
				Set<String> strings = entry.getKey();
				List<MergerFile> paths = entry.getValue();
				PlatformId id = new PlatformId(strings);
				for (MergerFile mergerFile : paths) {
					this.populate(outputSystem, classes, java, typeSolver, strings, id, mergerFile);
				}
			}

			for (Map.Entry<String, Collection<Platformed<Path>>> entry : classes.asMap().entrySet()) {
				String filePath = entry.getKey();
				Collection<Platformed<Path>> variants = entry.getValue();
				List<RawPlatformClass> platformClasses = new ArrayList<>();
				for (Platformed<Path> variant : variants) {
					platformClasses.addAll(this.from(variant));
				}

				ClassNode target = new ClassNode();
				for (Merger merger : this.mergers) {
					merger.merge(platformClasses, target, this.combinationsData, this.handler);
				}

				Path path = outputSystem.getPath(filePath);
				Files.createDirectories(path.getParent());
				ClassWriter writer = new ClassWriter(Opcodes.ASM9);
				target.accept(writer);
				Files.write(path, writer.toByteArray());
			}

		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		} finally {
			List<IOException> exceptions = null;
			if(closeMergerFiles) {
				for (List<MergerFile> value : Iterables.concat(this.resources.values(), this.toMerge.values())) {
					for (MergerFile mergerFile : value) {
						try {
							mergerFile.close();
						} catch (IOException e) {
							if(exceptions == null) exceptions = new ArrayList<>();
							exceptions.add(e);
						}
					}
				}
			}
			if(exceptions != null) {
				Iterator<IOException> iterator = exceptions.iterator();
				while (iterator.hasNext()) {
					IOException exception = iterator.next();
					if(!iterator.hasNext()) { // if last
						throw new RuntimeException(exception);
					} else {
						exception.printStackTrace();
					}
				}
			}
		}
	}

	private List<RawPlatformClass> from(Platformed<Path> classFile) throws IOException {
		try(InputStream stream = new BufferedInputStream(Files.newInputStream(classFile.val))) {
			ClassReader reader = new ClassReader(stream);
			ClassNode node = new ClassNode(Opcodes.ASM9);
			reader.accept(node, 0);
			// todo parse annotations
			List<RawPlatformClass> cls = new ArrayList<>();
			boolean visited = false;
			if(node.invisibleAnnotations != null) {
				for (AnnotationNode annotation : node.invisibleAnnotations) {
					PlatformId id = this.handler.parseClassPlatforms(annotation);
					if(id != null) {
						visited = true;
						Set<String> newNames = ListOrderedSet.listOrderedSet(id.names);
						newNames.addAll(classFile.id.names);
						cls.add(new RawPlatformClass(new PlatformId(newNames), node, null));
					}
				}
			}
			if(!visited) cls.add(new RawPlatformClass(classFile.id, node, null));
			return cls;
		}
	}

	private void populate(FileSystem outputSystem,
			Multimap<String, Platformed<Path>> classes,
			@Nullable Multimap<String, Platformed<Path>> java,
			@Nullable Map<Set<String>, CombinedTypeSolver> typeSolver,
			Set<String> strings,
			PlatformId id,
			MergerFile mergerFile) throws IOException, URISyntaxException {
		try (FileSystem resourceOut = mergerFile.resourcesOutput == null ? null : FileSystems.newFileSystem(new URI("jar:" + mergerFile.resourcesOutput.toUri()), AmalgamationIO.CREATE_ZIP)) {
			FileSystem trueOut = resourceOut == null ? outputSystem : resourceOut;
			CombinedTypeSolver combined = typeSolver == null ? null : typeSolver.computeIfAbsent(strings, s -> {
				CombinedTypeSolver solver = new CombinedTypeSolver();
				solver.add(new ClassLoaderTypeSolver(ClassLoader.getSystemClassLoader()));
				return solver;
			});
			if(typeSolver != null) {
				mergerFile.classpath.forEach(path -> combined.add(new PathTypeSolver(path)));
			}
			for (Path path : mergerFile.input) {
				if(typeSolver != null) {
					combined.add(new PathTypeSolver(path));
				}
				for (Path file : UnsafeIterable.walkFiles(path)) {
					String name = file.toString();
					if (name.endsWith(".class")) {
						classes.put(name, new Platformed<>(id, file));
					} else if(java != null && name.endsWith(".java")) {
						java.put(name, new Platformed<>(id, file));
					} else {
						Path out = trueOut.getPath(name);
						Files.createDirectories(out.getParent());
						Files.copy(file, out);
					}
				}
			}
		}
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
}
