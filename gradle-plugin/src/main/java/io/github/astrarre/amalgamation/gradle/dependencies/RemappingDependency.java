package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.MappingUtil;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import net.devtech.zipio.processors.zip.PostZipProcessor;
import net.devtech.zipio.stage.LinkedProcessTransform;
import net.devtech.zipio.stage.ZipTransform;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.NotNull;

import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;

@SuppressWarnings({
		"unchecked",
		"UnstableApiUsage"
})
public class RemappingDependency extends AbstractSelfResolvingDependency implements ZipProcessable {
	public static final Field EXECUTOR;
	static {
		try {
			EXECUTOR = TinyRemapper.class.getDeclaredField("threadPool");
			EXECUTOR.setAccessible(true);
		} catch(NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
	private final List<Dependency> inputsLocal, inputsGlobal;
	private final List<Dependency> classpath;
	private Dependency mappings;
	private String from, to;

	public RemappingDependency(Project project) {
		super(project, "io.github.amalgamation", "test" /*todo see if unique name is needed*/, "0.0.0");
		this.inputsGlobal = new ArrayList<>();
		this.inputsLocal = new ArrayList<>();
		this.classpath = new ArrayList<>();
	}

	public RemappingDependency(Project project,
			String group,
			String name,
			String version,
			Dependency mappings,
			String from,
			String to,
			List<Dependency> inputsLocal,
			List<Dependency> inputsGlobal,
			List<Dependency> classpath) {
		super(project, group, name, version);
		this.mappings = mappings;
		this.from = from;
		this.to = to;
		this.inputsLocal = inputsLocal;
		this.inputsGlobal = inputsGlobal;
		this.classpath = classpath;
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

	// todo remapAndClasspath
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
	public Iterable<Path> resolvePaths() throws IOException {
		return ZipProcessable.super.resolvePaths();
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

	@Override
	public ZipProcess process() {
		// todo autoappend files that exist to classpath
		ZipProcessBuilder builder = ZipProcess.builder();
		MappingSet mappings = MappingSet.create();
		List<File> resolved = new ArrayList<>();
		for(File file : this.resolve(List.of(this.mappings))) {
			MappingUtil.loadMappings(mappings, file, this.from, this.to);
			resolved.add(file);
		}

		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(MappingUtil.createMappingProvider(mappings)).build();
		for(ZipTransform transform : ZipProcessable.add(this.project, builder, this.classpath, path -> null)) {
			transform.setPreEntryProcessor(buffer -> {
				if(buffer.path().endsWith(".class")) {
					ByteBuffer read = buffer.read();
					remapper.readFileToClassPath(null, buffer.path(), read.array(), read.arrayOffset(), read.position());
				}
				return ProcessResult.HANDLED; // prevent classpath from being written
			});
		}

		this.extracted(builder, resolved, remapper, this.inputsGlobal, true);
		this.extracted(builder, resolved, remapper, this.inputsLocal, false);

		return builder;
	}

	private void extracted(ZipProcessBuilder builder, List<File> map, TinyRemapper remapper, List<Dependency> inputs, boolean global) {
		for(var transform : ZipProcessable.add(this.project, builder, inputs, path -> this.getRemapFile(map, path, global))) {
			if(transform instanceof LinkedProcessTransform t) {
				Map<Path, Remap> cache = new HashMap<>();
				var proc = (Function<Path, ?>) p -> cache.computeIfAbsent(p, ig -> new Remap(remapper));

				// hm problemo.
				t.setPreEntryProcessor((Function) proc);
				t.setPostZipProcessor((Function) proc);
			} else {
				Remap remap = new Remap(remapper);
				transform.setPreEntryProcessor(remap);
				transform.setPostZipProcessor(remap);
			}
		}
	}

	@NotNull
	private Path getRemapFile(List<File> map, Path path, boolean global) {
		Hasher hasher = Hashing.sha256().newHasher();
		AmalgIO.hash(hasher, map);
		hasher.putUnencodedChars(this.from);
		hasher.putUnencodedChars(this.to);
		try {
			hasher.putUnencodedChars(path.toRealPath().toString());
		} catch(IOException e) {
			throw U.rethrow(e);
		}
		String dir = AmalgIO.hash(hasher);
		Path main = AmalgIO.cache(this.project, global);
		Path at = main.resolve("remaps").resolve(this.mappings.getName() + "-" + this.mappings.getVersion());
		try {
			Files.createDirectories(at);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return at.resolve(dir + "-" + path.getFileName());
	}

	@Override
	public Dependency copy() {
		return new RemappingDependency(
				this.project,
				this.group,
				this.name,
				this.version,
				this.mappings,
				this.from,
				this.to,
				new ArrayList<>(this.inputsLocal),
				new ArrayList<>(this.inputsGlobal),
				new ArrayList<>(this.classpath));
	}
}
