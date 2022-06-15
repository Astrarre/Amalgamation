package io.github.astrarre.amalgamation.gradle.utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

public class Mappings {
	public static final Map<Path, MemoryMappingTree> MAPPINGS_CACHE = new HashMap<>();

	public static MappingSet toLorenz(Path path, String from, String to) throws IOException {
		var map = Mappings.from(path, from, to);
		MappingSet set = MappingSet.create();
		Mappings.loadMappings(set, map);
		return set;
	}

	public static Namespaced from(Path file, String from, String to) throws IOException {
		return new Namespaced(read(file), from, to);
	}

	public static Namespaced read(Project project, Dependency dependency, String from, String to) throws IOException {
		Path mappings = AmalgIO.resolveFile(project, dependency).toPath();
		return Mappings.from(mappings, from, to);
	}

	public static MemoryMappingTree read(Path file) throws IOException {
		return MAPPINGS_CACHE.computeIfAbsent(file.toRealPath(), f -> {
			MemoryMappingTree tree = new MemoryMappingTree();
			if(f.toString().endsWith(".jar") || f.toString().endsWith(".zip")) {
				try(FileSystem fileSystem = FileSystems.newFileSystem(f, (ClassLoader) null)) {
					MappingReader.read(fileSystem.getPath("/mappings/mappings.tiny"), tree);
				} catch(IOException e) {
					throw Err.rethrow(e);
				}
			} else {
				try {
					MappingReader.read(f, tree);
				} catch(IOException e) {
					throw Err.rethrow(e);
				}
			}
			return tree;
		});
	}

	public static IMappingProvider from(List<Namespaced> mappings) {
		return out -> {
			for(Namespaced mapping : mappings) {
				MappingTree tree = mapping.tree;
				int from = mapping.fromI, to = mapping.toI;
				for(MappingTree.ClassMapping cls : tree.getClasses()) {
					String name = cls.getName(from);
					String deobfC = cls.getName(to);
					if(deobfC != null) {
						out.acceptClass(name, deobfC);
					}
					for(MappingTree.MethodMapping method : cls.getMethods()) {
						String deobfM = method.getName(to);
						if(deobfM != null) {
							IMappingProvider.Member m = new IMappingProvider.Member(name, method.getName(from), method.getDesc(from));
							out.acceptMethod(m, deobfM);
							for(MappingTree.MethodArgMapping arg : method.getArgs()) {
								String argName = arg.getName(to);
								if(argName != null) {
									out.acceptMethodArg(m, arg.getLvIndex(), argName);
								}
							}
						}
					}

					for(MappingTree.FieldMapping field : cls.getFields()) {
						String deobfF = field.getName(to);
						if(deobfF != null) {
							IMappingProvider.Member f = new IMappingProvider.Member(name, field.getName(from), field.getDesc(from));
							out.acceptField(f, deobfF);
						}
					}
				}
			}
		};
	}

	public static void loadMappings(MappingSet set, Namespaced namespaced) {
		int to = namespaced.toI(), from = namespaced.fromI();
		for(MappingTree.ClassMapping cls : namespaced.tree().getClasses()) {
			String unmapped = cls.getName(to);
			var top = set.getOrCreateTopLevelClassMapping(cls.getName(from));
			if(unmapped != null) {
				top.setDeobfuscatedName(unmapped);
			}
			for(MappingTree.MethodMapping method : cls.getMethods()) {
				String name = method.getName(to);
				if(name != null) {
					top.getOrCreateMethodMapping(method.getName(from), method.getDesc(from)).setDeobfuscatedName(name);
				}
			}
			for(MappingTree.FieldMapping field : cls.getFields()) {
				String name = field.getName(to);
				if(name != null) {
					top.getOrCreateFieldMapping(field.getName(from), field.getDesc(from)).setDeobfuscatedName(name);
				}
			}
		}
	}

	static <T, C> T orDef(T val, C context, Function<C, T> apply) {
		return val == null ? apply.apply(context) : val;
	}

	public record Namespaced(MappingTree tree, String from, String to, int fromI, int toI) {
		public Namespaced(MappingTree tree, String from, String to) {
			this(tree, from, to, tree.getNamespaceId(from), tree.getNamespaceId(to));
		}
	}
}
