package io.github.astrarre.amalgamation.gradle.dependencies.remap.binary;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.ZipRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;

public class TinyRemapperImpl implements AmalgRemapper {
	/**
	 * Matches the new local variable naming format introduced in 21w37a.
	 */
	private static final Pattern NEW_MINECRAFT_LOCAL_PATTERN = Pattern.compile("\\$\\$\\d+");

	final Consumer<TinyRemapper.Builder> configurator;
	TinyRemapper remapper;

	public TinyRemapperImpl(Consumer<TinyRemapper.Builder> configurator) {this.configurator = configurator;}

	@Override
	public boolean requiresClasspath() {
		return true;
	}

	@Override
	public boolean hasPostStage() {
		return true;
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
		this.configurator.accept(builder);
		this.remapper = builder.build();
	}

	@Override
	public ZipRemapper createNew() {
		return new ZipRemapper() {
			final InputTag tag = TinyRemapperImpl.this.remapper.createInputTag();

			@Override
			public boolean visitEntry(VirtualZipEntry entry, boolean isClasspath) {
				String path = entry.path();
				if(path.endsWith(".class")) {
					ByteBuffer data = entry.read();
					if(isClasspath) {
						TinyRemapperImpl.this.remapper.readFileToClassPath(this.tag, path, data.array(), data.arrayOffset(), data.limit());
					} else {
						TinyRemapperImpl.this.remapper.readFileToInput(this.tag, path, data.array(), data.arrayOffset(), data.limit());
					}
					return true;
				}
				return false;
			}

			@Override
			public void acceptPost(ZipOutput output) {
				TinyRemapperImpl.this.remapper.apply((s, b) -> output.write(s + ".class", ByteBuffer.wrap(b)), this.tag);
			}
		};
	}
}
