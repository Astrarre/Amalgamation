package io.github.astrarre.amalgamation.gradle.dependencies.remap.binary;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import it.unimi.dsi.fastutil.Pair;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.VirtualPath;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
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
	TinyRemapper remapper;

	public TinyRemapperImpl(Consumer<TinyRemapper.Builder> configurator) {
		this.configurator = configurator;
	}

	@Override
	public boolean requiresClasspath() {
		return true;
	}
	
	@Override
	public void acceptClasspath(Set<Artifact> classpath) {
		for(Artifact artifact : classpath) {
			VirtualSource source = artifact.file.openOrThrow();
			InputTag tag = this.remapper.createInputTag();
			source.depthStream().filter(VirtualFile.class::isInstance).filter(p -> p.relativePath().endsWith(".class")).forEach(path -> {
				ByteBuffer data = ((VirtualFile) path).getContents();
				TinyRemapperImpl.this.remapper.readFileToClassPath(tag, path.relativePath(), data.array(), data.arrayOffset(), data.limit());
			});
		}
	}
	
	final List<Pair<VirtualPath, InputTag>> outputs = new ArrayList<>();
	
	@Override
	public void acceptRemaps(List<Pair<Artifact, Artifact>> fromTos) throws Exception {
		for(Pair<Artifact, Artifact> to : fromTos) {
			VirtualSource source = to.left().file.openAsSource();
			InputTag tag = this.remapper.createInputTag();
			this.outputs.add(Pair.of(to.right().file, tag));
			source.depthStream().filter(VirtualFile.class::isInstance).filter(p -> p.relativePath().endsWith(".java")).forEach(path -> {
				ByteBuffer data = ((VirtualFile) path).getContents();
				TinyRemapperImpl.this.remapper.readFileToInput(tag, path.relativePath(), data.array(), data.arrayOffset(), data.limit());
			});
		}
	}
	
	@Override
	public void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper) {
		IMappingProvider from = Mappings.from(list);
		TinyRemapper.Builder builder = TinyRemapper.newRemapper()
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
	
	@Override
	public void write() {
		for(Pair<VirtualPath, InputTag> output : this.outputs) {
			VirtualSink sink = AmalgIO.DISK_OUT.subsink(output.first());
			TinyRemapperImpl.this.remapper.apply((s, b) -> sink.write(sink.outputFile(s + ".class"), ByteBuffer.wrap(b)), output.right());
		}
		this.outputs.clear();
	}

	@Override
	public void hash(Hasher hasher) {
		hasher.putString("tiny remapper", StandardCharsets.UTF_8);
		if(this.configurator != null) {
			hasher.putString("trolled", StandardCharsets.UTF_8);
		}
	}
}
