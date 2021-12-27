/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2019-2021 FabricMC
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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.devtech.zipio.impl.util.U;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.struct.StructContext;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;

public final class FabricFernFlowerDecompiler implements LoomDecompiler {
	@Override
	public String name() {
		return "FernFlower";
	}

	@Override
	public void decompile(List<Entry> entries, DecompilationMetadata metaData) throws IOException {
		Map<String, Path> fileNames = new HashMap<>();
		List<Path> temporaryFile = new ArrayList<>();
		List<Entry> list = new ArrayList<>(entries);
		for(int i = 0; i < list.size(); i++) {
			LoomDecompiler.Entry entry = list.get(i);
			Path input = entry.input();
			Path put = fileNames.putIfAbsent(input.getFileName().toString(), input);
			if(put != null) {
				System.out.println("[Warn] duplicate file name between " + input.toAbsolutePath() + " and " + put.toAbsolutePath());
				System.out.println("[Warn] copying " + input.toAbsolutePath() + " to temporary file to circumvent fernflower api restriction");
				Path temp = Files.createTempFile("amalg_fernflower_hack"+i, ".jar");
				temporaryFile.add(temp);
				Files.copy(input, temp);
				list.set(i, new Entry(temp, entry.output(), entry.lineMapOutput()));
			}
		}

		final Map<String, Object> options = new HashMap<>(
				Map.of(
					IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1",
					IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",
					IFernflowerPreferences.REMOVE_SYNTHETIC, "1",
					IFernflowerPreferences.LOG_LEVEL, "trace",
					IFernflowerPreferences.THREADS, String.valueOf(metaData.numberOfThreads()),
					IFernflowerPreferences.INDENT_STRING, "\t",
					IFabricJavadocProvider.PROPERTY_NAME, new TinyJavadocProvider(metaData.javaDocs())
				)
		);

		options.putAll(metaData.options());

		var provider = new IBytecodeProvider() {
			final Map<String, FileSystem> systems = new ConcurrentHashMap<>();

			@Override
			public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
				try {
					if(internalPath == null) {
						return Files.readAllBytes(Paths.get(externalPath));
					}
					Path path = this.systems.computeIfAbsent(externalPath, s -> {
						try {
							return FileSystems.newFileSystem(Paths.get(s));
						} catch(IOException e) {
							throw U.rethrow(e);
						}
					}).getPath(internalPath);
					return Files.readAllBytes(path);
				} catch(Throwable t) {
					t.printStackTrace();
					throw U.rethrow(t);
				}
			}
		};

		IResultSaver saver = new AsyncResultSaver(list);
		Fernflower ff = new Fernflower(provider, saver, options, new FernflowerLogger(metaData.logger()));

		for (Path library : metaData.libraries()) {
			ff.addLibrary(library.toFile());
		}

		for(Entry entry : list) {
			ff.addSource(entry.input().toFile());
		}
		ff.decompileContext();

		IOException last = null;
		for(Path path : temporaryFile) {
			try {
				Files.delete(path);
			} catch(IOException e) {
				last = e;
				e.printStackTrace();
			}
		}
		for(FileSystem value : provider.systems.values()) {
			try {
				value.close();
			} catch(IOException e) {
				last = e;
				e.printStackTrace();
			}
		}

		if(last != null) {
			throw last;
		}
	}
}