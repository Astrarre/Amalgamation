/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.astrarre.amalgamation.gradle.dependencies.decomp;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.astrarre.amalgamation.gradle.utils.OS;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.gradle.workers.internal.WorkerDaemonClientsManager;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

public abstract class GenerateSourcesTask extends DefaultTask {
	public final LoomDecompiler decompiler;

	@InputFile
	public abstract RegularFileProperty getInputJar();

	@InputFiles
	public abstract ConfigurableFileCollection getClasspath();

	@OutputFile
	public abstract RegularFileProperty getOutputDestination();

	@OutputFile
	public abstract RegularFileProperty getLineMappedDestination();

	@InputFile
	public abstract RegularFileProperty getMappingsFile();

	@Input
	public abstract Property<String> getJavadocNamespace();

	/**
	 * Max memory for forked JVM in megabytes.
	 */
	@Input
	public abstract Property<Long> getMaxMemory();

	@Input
	public abstract MapProperty<String, String> getOptions();

	@Inject
	public abstract WorkerExecutor getWorkerExecutor();

	@Inject
	public abstract WorkerDaemonClientsManager getWorkerDaemonClientsManager();

	@Inject
	public GenerateSourcesTask(LoomDecompiler decompiler) {
		this.decompiler = decompiler;

		Objects.requireNonNull(getDecompilerConstructor(this.decompiler.getClass().getCanonicalName()),
				"%s must have a no args constructor".formatted(this.decompiler.getClass().getCanonicalName()));

		getOutputs().upToDateWhen((o) -> false);
		getMaxMemory().convention(4096L).finalizeValueOnRead();
		getOptions().finalizeValueOnRead();
	}

	@TaskAction
	public void run() throws IOException {

		if (!OS.isUnixDomainSocketsSupported()) {
			getProject().getLogger().warn("Decompile worker logging disabled as Unix Domain Sockets is not supported on your operating system.");

			doWork(null);
			return;
		}

		// Set up the IPC path to get the log output back from the forked JVM
		final Path ipcPath = Files.createTempFile("loom", "ipc");
		Files.deleteIfExists(ipcPath);

		try (ThreadedProgressLoggerConsumer loggerConsumer = new ThreadedProgressLoggerConsumer(getProject(), decompiler.name(), "Decompiling minecraft sources");
				IPCServer logReceiver = new IPCServer(ipcPath, loggerConsumer)) {
			doWork(ipcPath);
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to shutdown log receiver", e);
		} finally {
			Files.deleteIfExists(ipcPath);
		}
	}

	private void doWork(@Nullable Path ipcPath) {
		final String jvmMarkerValue = UUID.randomUUID().toString();
		final WorkQueue workQueue = createWorkQueue(jvmMarkerValue);

		workQueue.submit(DecompileAction.class, params -> {
			params.getDecompilerClass().set(decompiler.getClass().getCanonicalName());

			params.getOptions().set(getOptions());

			params.getInputJar().set(getInputJar());
			params.getSourcesDestinationJar().set(getOutputDestination());
			try {
				params.getLinemap().set(File.createTempFile("amalg_linemap", ".lmap"));
			} catch(IOException e) {
				e.printStackTrace();
			}
			params.getLinemapJar().set(getLineMappedDestination());
			params.getMappings().set(getMappingsFile());
			params.getJavadocNamespace().set(getJavadocNamespace());

			if (ipcPath != null) {
				params.getIPCPath().set(ipcPath.toFile());
			}

			params.getClassPath().setFrom(getClasspath());
		});

		try {
			workQueue.await();
		} finally {
			if (useProcessIsolation()) {
				boolean stopped = WorkerDaemonClientsManagerHelper.stopIdleJVM(getWorkerDaemonClientsManager(), jvmMarkerValue);

				if (!stopped) {
					throw new RuntimeException("Failed to stop decompile worker JVM");
				}
			}
		}
	}

	private WorkQueue createWorkQueue(String jvmMarkerValue) {
		if (!useProcessIsolation()) {
			return getWorkerExecutor().noIsolation();
		}

		return getWorkerExecutor().processIsolation(spec -> {
			spec.forkOptions(forkOptions -> {
				forkOptions.setMaxHeapSize("%dm".formatted(getMaxMemory().get()));
				forkOptions.systemProperty(WorkerDaemonClientsManagerHelper.MARKER_PROP, jvmMarkerValue);
			});
		});
	}

	private boolean useProcessIsolation() {
		// Useful if you want to debug the decompiler, make sure you run gradle with enough memory.
		return !Boolean.getBoolean("fabric.loom.genSources.debug");
	}

	public interface DecompileParams extends WorkParameters {
		Property<String> getDecompilerClass();

		MapProperty<String, String> getOptions();

		RegularFileProperty getInputJar();
		RegularFileProperty getSourcesDestinationJar();
		RegularFileProperty getLinemap();
		RegularFileProperty getLinemapJar();
		RegularFileProperty getMappings();

		Property<String> getJavadocNamespace();

		RegularFileProperty getIPCPath();

		ConfigurableFileCollection getClassPath();
	}

	public abstract static class DecompileAction implements WorkAction<DecompileParams> {
		@Override
		public void execute() {
			if (!getParameters().getIPCPath().isPresent() || !OS.isUnixDomainSocketsSupported()) {
				// Does not support unix domain sockets, print to sout.
				try {
					doDecompile(System.out::println);
				} catch(IOException e) {
					e.printStackTrace();
				}
				return;
			}

			final Path ipcPath = getParameters().getIPCPath().get().getAsFile().toPath();

			try (IPCClient ipcClient = new IPCClient(ipcPath)) {
				doDecompile(new ThreadedSimpleProgressLogger(ipcClient));
			} catch (Exception e) {
				throw new RuntimeException("Failed to decompile", e);
			}
		}

		private void doDecompile(IOStringConsumer logger) throws IOException {
			final Path inputJar = getParameters().getInputJar().get().getAsFile().toPath();
			final Path sourcesDestinationJar = getParameters().getSourcesDestinationJar().get().getAsFile().toPath();
			final Path linemap = getParameters().getLinemap().get().getAsFile().toPath();
			final Path linemapJar = getParameters().getLinemapJar().get().getAsFile().toPath();

			final LoomDecompiler decompiler;

			try {
				decompiler = getDecompilerConstructor(getParameters().getDecompilerClass().get()).newInstance();
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Failed to create decompiler", e);
			}


			MemoryMappingTree read = Mappings.read(getParameters().getMappings().get().getAsFile().toPath());
			DecompilationMetadata metadata = new DecompilationMetadata(
					Runtime.getRuntime().availableProcessors(),
					new Mappings.Namespaced(read, read.getSrcNamespace(), getParameters().getJavadocNamespace().get()),
					getLibraries(),
					logger,
					getParameters().getOptions().get()
			);

			decompiler.decompile(
					inputJar,
					sourcesDestinationJar,
					linemap,
					metadata
			);

			// Close the decompile loggers
			try {
				metadata.logger().accept(ThreadedProgressLoggerConsumer.CLOSE_LOGGERS);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to close loggers", e);
			}

			if (Files.exists(linemap)) {
				try {
					// Line map the actually jar used to run the game, not the one used to decompile
					remapLineNumbers(metadata.logger(), inputJar, linemap, linemapJar);
					Files.delete(linemap);
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to remap line numbers", e);
				}
			}
		}

		private void remapLineNumbers(IOStringConsumer logger, Path oldCompiledJar, Path linemap, Path linemappedJarDestination) throws IOException {
			LineNumberRemapper remapper = new LineNumberRemapper();
			remapper.readMappings(linemap.toFile());

			try (FileSystemUtil.Delegate inFs = FileSystemUtil.getJarFileSystem(oldCompiledJar.toFile(), true);
					FileSystemUtil.Delegate outFs = FileSystemUtil.getJarFileSystem(linemappedJarDestination.toFile(), true)) {
				remapper.process(logger, inFs.get().getPath("/"), outFs.get().getPath("/"));
			}
		}

		private Collection<Path> getLibraries() {
			return getParameters().getClassPath().getFiles().stream().map(File::toPath).collect(Collectors.toSet());
		}
	}

	private static Constructor<LoomDecompiler> getDecompilerConstructor(String clazz) {
		try {
			//noinspection unchecked
			return (Constructor<LoomDecompiler>) Class.forName(clazz).getConstructor();
		} catch (NoSuchMethodException | ClassNotFoundException e) {
			return null;
		}
	}
}