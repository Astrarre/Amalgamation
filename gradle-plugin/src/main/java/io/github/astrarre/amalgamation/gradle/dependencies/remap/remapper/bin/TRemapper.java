package io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.bin;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AbstractRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.ZipOutput;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;

public class TRemapper extends AbstractRemapper {
	public static final Field EXECUTOR;

	static {
		try {
			EXECUTOR = TinyRemapper.class.getDeclaredField("threadPool");
			EXECUTOR.setAccessible(true);
		} catch(NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	TinyRemapper remapper;

	@Override
	public void init(List<Mappings.Namespaced> mappings) {
		super.init(mappings);
		this.remapper = TinyRemapper.newRemapper().withMappings(Mappings.from(mappings)).build();
		try {
			EXECUTOR.set(this.remapper, Constants.SERVICE);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Remap remap() {
		return new RemapExt(this.remapper.createInputTag());
	}

	@Override
	protected void readFileToClassPath(String classFile, ByteBuffer data) {
		this.remapper.readFileToClassPath(null, classFile, data.array(), data.arrayOffset(), data.position());
	}

	@Override
	protected void readFileToInput(RemapImpl remapData, String path, ByteBuffer data) {
		this.remapper.readFileToInput(((RemapExt)remapData).tag, path, data.array(), data.arrayOffset(), data.position());
	}

	@Override
	protected void write(RemapImpl remapData, ZipOutput output) {
		this.remapper.apply((s, b) -> output.write(s + ".class", ByteBuffer.wrap(b)), ((RemapExt)remapData).tag);
	}

	public class RemapExt extends RemapImpl {
		final InputTag tag;
		RemapExt(InputTag input) {
			super();
			this.tag = input;
		}
	}
}
