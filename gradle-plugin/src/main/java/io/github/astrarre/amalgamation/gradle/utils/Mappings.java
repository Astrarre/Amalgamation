package io.github.astrarre.amalgamation.gradle.utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.devtech.zipio.impl.util.U;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

public class Mappings {
	public static final Map<Path, MemoryMappingTree> MAPPINGS_CACHE = new HashMap<>();

	public static MappingSet fromLorenz(Path path, String from, String to) throws IOException {
		var map = Mappings.from(path, from, to);
		MappingSet set = MappingSet.create();
		Mappings.loadMappings(set, map);
		return set;
	}

	public static Namespaced from(Path file, String from, String to) throws IOException {
		return new Namespaced(read(file), from, to);
	}

	public static Namespaced read(Project project, Dependency dependency, String from, String to) throws IOException {
		Path mappings = AmalgIO.resolve(project, dependency).toPath();
		return Mappings.from(mappings, from, to);
	}

	public static MemoryMappingTree read(Path file) throws IOException {
		return MAPPINGS_CACHE.computeIfAbsent(file.toRealPath(), f -> {
			MemoryMappingTree tree = new MemoryMappingTree();
			if(f.toString().endsWith(".jar") || f.toString().endsWith(".zip")) {
				try(FileSystem fileSystem = FileSystems.newFileSystem(f, (ClassLoader) null)) {
					MappingReader.read(fileSystem.getPath("/mappings/mappings.tiny"), tree);
				} catch(IOException e) {
					throw U.rethrow(e);
				}
			} else {
				try {
					MappingReader.read(f, tree);
				} catch(IOException e) {
					throw U.rethrow(e);
				}
			}
			return tree;
		});
	}

	public static IMappingProvider from(List<Namespaced> mappings) {
		return out -> {
			for(Namespaced mapping : mappings) {
				MemoryMappingTree tree = mapping.tree;
				int from = mapping.fromI, to = mapping.toI;
				for(MappingTree.ClassMapping cls : tree.getClasses()) {
					String name = cls.getName(from);
					out.acceptClass(name, cls.getName(to));
					for(MappingTree.MethodMapping method : cls.getMethods()) {
						IMappingProvider.Member m = new IMappingProvider.Member(name, method.getName(from), method.getDesc(from));
						out.acceptMethod(m, method.getName(to));
					}

					for(MappingTree.FieldMapping field : cls.getFields()) {
						IMappingProvider.Member m = new IMappingProvider.Member(name, field.getName(from), field.getDesc(from));
						out.acceptField(m, field.getName(to));
					}
				}
			}
		};
	}

	public static void loadMappings(MappingSet set, Namespaced namespaced) {
		int to = namespaced.toI(), from = namespaced.fromI();
		for(MappingTree.ClassMapping cls : namespaced.tree().getClasses()) {
			var top = set.getOrCreateTopLevelClassMapping(cls.getName(from)).setDeobfuscatedName(cls.getName(to));
			for(MappingTree.MethodMapping method : cls.getMethods()) {
				top.getOrCreateMethodMapping(method.getName(from), method.getDesc(from)).setDeobfuscatedName(method.getName(to));
			}
			for(MappingTree.FieldMapping field : cls.getFields()) {
				top.getOrCreateFieldMapping(field.getName(from), field.getDesc(from)).setDeobfuscatedName(field.getName(to));
			}
		}
	}

	public record Namespaced(MemoryMappingTree tree, String from, String to, int fromI, int toI) {
		public Namespaced(MemoryMappingTree tree, String from, String to) {
			this(tree, from, to, tree.getNamespaceId(from), tree.getNamespaceId(to));
		}
	}
}
