package io.github.astrarre.amalgamation.gradle.dependencies.refactor.remap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.refactor.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Constants;
import io.github.astrarre.amalgamation.gradle.utils.MappingUtil;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import net.devtech.zipio.processors.zip.PostZipProcessor;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemapDependency extends ZipProcessDependency {
	public static final Field EXECUTOR;

	static {
		try {
			EXECUTOR = TinyRemapper.class.getDeclaredField("threadPool");
			EXECUTOR.setAccessible(true);
		} catch(NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
	
	private final List<Dependency> inputsLocal = new ArrayList<>(), inputsGlobal = new ArrayList<>();

	private final List<Dependency> classpath = new ArrayList<>();
	private Dependency mappings;
	private String from, to;

	public RemapDependency(Project project) {
		super(project, "io.github.astrarre.amalgamation", "remapped", "1.0.0");
	}

	/**
	 * this layers mappings only, it does not map each mapping one after the other!
	 *
	 * @param object the dependency
	 * @param from the origin namespace
	 * @param to the destination namespace
	 */
	public Dependency mappings(Object object, String from, String to) {
		if(this.mappings == null) {
			this.mappings = this.project.getDependencies().create(object);
			this.from = from;
			this.to = to;
			return this.mappings;
		}
		throw new IllegalStateException("cannot layer mappings like that yet");
	}


	public void remap(Object object, boolean useGlobalCache) {
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(this.project.getDependencies().create(object));
	}

	public void remap(Object object, boolean useGlobalCache, Closure<ModuleDependency> config) {
		(useGlobalCache ? this.inputsGlobal : this.inputsLocal).add(this.project.getDependencies().create(object, config));
	}

	public void classpath(Object object) {
		this.classpath.add(this.project.getDependencies().create(object));
	}

	public void classpath(Object object, Closure<ModuleDependency> config) {
		this.classpath.add(this.project.getDependencies().create(object, config));
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		for(Dependency dependency : this.classpath) {
			this.hashDep(hasher, dependency);
		}

		for(Dependency dependency : this.inputsGlobal) {
			this.hashDep(hasher, dependency);
		}

		for(Dependency dependency : this.inputsLocal) {
			this.hashDep(hasher, dependency);
		}

		this.hashDep(hasher, this.mappings);
		hasher.putString(this.from, StandardCharsets.UTF_8);
		hasher.putString(this.to, StandardCharsets.UTF_8);
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		return AmalgIO.cache(this.project, this.inputsLocal.isEmpty()).resolve("remaps").resolve(this.from + "-" + this.to + "_" + AmalgIO.b64(hash));
	}

	@Override
	protected void add(ZipProcessBuilder builder, Path resolvedPath, boolean isOutdated) throws IOException {
		Hasher hasher = HASHING.newHasher();
		hasher.putString(this.from, StandardCharsets.UTF_8);
		hasher.putString(this.to, StandardCharsets.UTF_8);
		this.hashDep(hasher, this.mappings);
		byte[] mappingsHash = hasher.hash().asBytes();
		Classpath classpath;
		Supplier<Remap> remap;
		if(isOutdated) {
			MappingSet mappings = MappingSet.create();
			for(File file : this.resolve(List.of(this.mappings))) {
				MappingUtil.loadMappings(mappings, file, this.from, this.to);
			}

			TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(MappingUtil.createMappingProvider(mappings)).build();
			try {
				EXECUTOR.set(remapper, Constants.SERVICE);
			} catch(IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			remap = () -> new Remap(remapper);
			classpath = new Classpath(remapper);
		} else {
			remap = () -> null;
			classpath = null;
		}

		// todo add classpath

		for(Dependency dependency : this.inputsGlobal) {
			var dep = new SingleRemapDependency(this.project, remap, classpath, from, to, mappingsHash, dependency, true, isOutdated);
			dep.add(builder, dep.getPath(), isOutdated && dep.isOutdated());
		}
		for(Dependency dependency : this.inputsLocal) {
			var dep = new SingleRemapDependency(this.project, remap, classpath, from, to, mappingsHash, dependency, false, isOutdated);
			dep.add(builder, dep.getPath(), isOutdated && dep.isOutdated());
		}
	}

	static final class Classpath implements ZipEntryProcessor {
		final TinyRemapper remapper;

		Classpath(TinyRemapper remapper) {
			this.remapper = remapper;
		}

		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			if(buffer.path().endsWith(".class")) {
				ByteBuffer read = buffer.read();
				remapper.readFileToClassPath(null, buffer.path(), read.array(), read.arrayOffset(), read.position());
			}
			return ProcessResult.HANDLED; // prevent classpath from being written
		}
	}

	static final class Remap implements ZipEntryProcessor, PostZipProcessor {
		final TinyRemapper remapper;
		final InputTag input;

		Remap(TinyRemapper remapper) {
			this.remapper = remapper;
			this.input = remapper.createInputTag();
		}

		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			if(buffer.path().endsWith(".class")) {
				ByteBuffer read = buffer.read();
				this.remapper.readFileToInput(this.input, buffer.path(), read.array(), read.arrayOffset(), read.limit());
			} else {
				buffer.copyToOutput();
			}
			return ProcessResult.HANDLED;
		}

		@Override
		public void apply(ZipOutput output) {
			this.remapper.apply((s, b) -> output.write(s + ".class", ByteBuffer.wrap(b)), this.input);
		}
	}
}
