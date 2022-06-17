package io.github.astrarre.amalgamation.gradle.dependencies.remap.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import net.devtech.betterzipfs.ZipFS;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;

public class TinyRemapperImpl implements AmalgamationRemapper {
	/**
	 * Matches the new local variable naming format introduced in 21w37a.
	 */
	private static final Pattern NEW_MINECRAFT_LOCAL_PATTERN = Pattern.compile("\\$\\$\\d+");
	
	final Consumer<TinyRemapper.Builder> configurator;
	record Outputs(FSRef left, InputTag right) {}
	final List<Outputs> outputs = new ArrayList<>(); // todo concurrent list for nested
	public TinyRemapper remapper;
	
	public TinyRemapperImpl(Consumer<TinyRemapper.Builder> configurator) {
		this.configurator = configurator;
	}
	
	@Override
	public void hash(Hasher hasher) {
		hasher.putString("tiny remapper", StandardCharsets.UTF_8);
		if(this.configurator != null) {
			hasher.putString("trolled", StandardCharsets.UTF_8);
		}
	}
	
	@Override
	public boolean requiresClasspath() {
		return true;
	}
	
	@Override
	public RemapTask createTask(RemapTarget target) {
		InputTag tag = this.remapper.createInputTag();
		this.outputs.add(new Outputs(target.toFs(), tag));
		return (srcFs, srcPath, dstFs, handled) -> {
			if(srcPath.toString().endsWith(".class")) { // todo handle nested jars
				TinyRemapperImpl.this.remapper.readInputFile(tag, srcPath);
				return true;
			} else {
				return false;
			}
		};
	}
	
	@Override
	public RemapTask classpathTask(Artifact from, FSRef fromFs) {
		InputTag tag = this.remapper.createInputTag();
		return (srcFs, srcPath, dstFs, handled) -> {
			if(srcPath.toString().endsWith(".class")) {
				TinyRemapperImpl.this.remapper.readClasspathFile(tag, srcPath);
			}
			return false;
		};
	}
	
	@Override
	public void write() {
		Map<InputTag, FSRef> map = this.outputs.stream().collect(Collectors.toMap(Outputs::right, Outputs::left));
		TinyRemapperImpl.this.remapper.apply((tags, internalName, bytecode) -> {
			try {
				Path first = map.get(tags[0]).getPath(internalName + ".class");
				AmalgIO.createParent(first);
				AmalgIO.write(first, ByteBuffer.wrap(bytecode));
				ZipFS.flush(first);
				for(int i = 1; i < tags.length; i++) {
					Path path = map.get(tags[i]).getPath(internalName + ".class");
					AmalgIO.createParent(first);
					Files.copy(first, path);
				}
			} catch(IOException e) {
				throw Err.rethrow(e);
			}
		});
		TinyRemapperImpl.this.remapper.finish();
		this.outputs.clear();
	}
	
	@Override
	public void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper) {
		IMappingProvider from = Mappings.from(list);
		TinyRemapper.Builder builder = TinyRemapper
				.newRemapper()
				.withMappings(from)
				.keepInputData(true)
				.rebuildSourceFilenames(true)
				.renameInvalidLocals(true)
				.invalidLvNamePattern(NEW_MINECRAFT_LOCAL_PATTERN)
				.useForkJoinPool();
		if(this.configurator != null) {
			this.configurator.accept(builder);
		}
		this.remapper = builder.build();
	}
}
