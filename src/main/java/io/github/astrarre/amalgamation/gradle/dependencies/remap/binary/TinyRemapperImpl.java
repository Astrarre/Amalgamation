package io.github.astrarre.amalgamation.gradle.dependencies.remap.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import io.github.astrarre.amalgamation.gradle.utils.zip.FSRef;
import it.unimi.dsi.fastutil.Pair;
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
	final List<Pair<FSRef, InputTag>> outputs = new ArrayList<>();
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
		this.outputs.add(Pair.of(target.toFs(), tag));
		return (srcFs, srcPath, dstFs, handled) -> {
			if(srcPath.toString().endsWith(".class")) {
				TinyRemapperImpl.this.remapper.readFileToInput(tag, srcPath);
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
			TinyRemapperImpl.this.remapper.readFileToClassPath(tag, srcPath);
			return false;
		};
	}
	
	@Override
	public void write() {
		for(Pair<FSRef, InputTag> output : this.outputs) {
			TinyRemapperImpl.this.remapper.apply((s, b) -> {
				try {
					AmalgIO.write(output.first().getPath(s), ByteBuffer.wrap(b));
				} catch(IOException e) {
					throw Err.rethrow(e);
				}
			}, output.right());
		}
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
				.invalidLvNamePattern(NEW_MINECRAFT_LOCAL_PATTERN);
		if(this.configurator != null) {
			this.configurator.accept(builder);
		}
		this.remapper = builder.build();
	}
}
