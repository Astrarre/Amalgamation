package io.github.astrarre.merger.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.astrarre.merger.api.PlatformId;
import io.github.astrarre.merger.api.Platformed;
import io.github.astrarre.merger.impl.AccessMerger;
import io.github.astrarre.merger.impl.ClassMerger;
import io.github.astrarre.merger.impl.HeaderMerger;
import io.github.astrarre.merger.impl.InnerClassAttributeMerger;
import io.github.astrarre.merger.impl.InterfaceMerger;
import io.github.astrarre.merger.Merger;
import io.github.astrarre.merger.impl.SuperclassMerger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class MergeTest {
	public static void main(String[] args) throws IOException {
		try(FileSystem path = FileSystems.newFileSystem(Paths.get(args[0]), null)) {
			for (Path directory : path.getRootDirectories()) {
				Path ca = directory.resolve("tests/ClassA.class");
				Path cb = directory.resolve("tests/ClassB.class");
				ClassNode aNode = new ClassNode(), bNode = new ClassNode();
				ClassReader readerA = new ClassReader(Files.newInputStream(ca));
				ClassReader readerB = new ClassReader(Files.newInputStream(cb));
				readerA.accept(aNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
				readerB.accept(bNode,  ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

				ClassNode merged = new ClassNode();
				PlatformId fabric = PlatformId.of("fabric"), forge = PlatformId.of("forge"), fabricClient = PlatformId.of("fabric", "client");
				Set<PlatformId> activePlatforms = new HashSet<>();
				activePlatforms.add(fabric);
				activePlatforms.add(forge);
				activePlatforms.add(fabricClient);

				List<Platformed<ClassNode>> nodes = new ArrayList<>();
				nodes.add(new Platformed<>(fabric, aNode));
				nodes.add(new Platformed<>(forge, bNode));
				//nodes.add(new Platformed<>(fabricClient, aNode));

				List<Merger> mergers = new ArrayList<>();
				mergers.add(new HeaderMerger(null));
				mergers.add(new SuperclassMerger(null));
				mergers.add(new InnerClassAttributeMerger(null));
				mergers.add(new ClassMerger(null));
				mergers.add(new InterfaceMerger(null));
				mergers.add(new AccessMerger(null));

				List<List<String>> combinations = new ArrayList<>();
				combinations.add(Arrays.asList("server", "client"));
				combinations.add(Arrays.asList("forge", "fabric", "spigot"));
				combinations.add(Arrays.asList("1.16.5", "1.16.4"));
				for (Merger merger : mergers) {
					merger.merge(activePlatforms, nodes, merged, combinations);
				}

				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
				merged.accept(writer);
				try(FileOutputStream fos = new FileOutputStream("yeet.class")) {
					fos.write(writer.toByteArray());
				}
			}
		}
	}
}
