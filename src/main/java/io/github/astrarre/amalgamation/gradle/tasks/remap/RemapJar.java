/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.astrarre.amalgamation.gradle.tasks.remap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Iterables;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.github.astrarre.amalgamation.gradle.tasks.remap.remap.AwResourceRemapper;
import io.github.astrarre.amalgamation.gradle.mixin.MixinExtensionReborn;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.jvm.tasks.Jar;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

public abstract class RemapJar extends Jar implements RemapTask {
	final List<OutputConsumerPath.ResourceRemapper> remappers = new ArrayList<>();
	boolean experimentalMixinRemapper = false;

	public RemapJar() {
	}

	public void addResourceRemapper(OutputConsumerPath.ResourceRemapper remapper) {
		this.remappers.add(remapper);
	}

	public void remapAw() {
		this.remappers.add(new AwResourceRemapper(() -> this.getMappings().get().get(0).to));
	}

	public void remapAw(String destNamespace) {
		this.remappers.add(new AwResourceRemapper(() -> destNamespace));
	}

	public void enableExperimentalMixinRemapper() {
		experimentalMixinRemapper = true;
		remappers.add(new OutputConsumerPath.ResourceRemapper() {
			@Override
			public boolean canTransform(TinyRemapper remapper, Path relativePath) {
				return relativePath.toString().endsWith(".mixins.json");
			}

			@Override
			public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper remapper) throws IOException {
				String content = Files.readString(relativePath);
				Files.writeString(destinationDirectory.resolve(relativePath), content.replaceFirst("\\{", "{\"refmap\": \"work/empty.refmap.json\","));
			}
		});
	}

	public void remap() throws IOException {
		FileCollection classpath = this.getClasspath().get();
		IMappingProvider from = Mappings.from(this.readMappings());
		TinyRemapper.Builder builder = TinyRemapper.newRemapper().withMappings(from);
		if(experimentalMixinRemapper) {
			builder.extension(new MixinExtensionReborn(this.getLogger()));
		}

		TinyRemapper remapper = builder
				.build();

		Path current = this.getCurrent();

		List<CompletableFuture<?>> futures = new ArrayList<>();
		futures.add(remapper.readInputsAsync(current));

		for(File file : classpath) {
			futures.add(remapper.readClassPathAsync(file.toPath()));
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		try(OutputConsumerPath ocp = new OutputConsumerPath.Builder(current).build()) {
			if(experimentalMixinRemapper) {
				try(FileSystem system = Jimfs.newFileSystem()) {
					Path path = system.getPath("empty.refmap.json");
					Files.writeString(path, "{\"mappings\": {},\"data\":{\"named:intermediary\":{}}}");
					ocp.addNonClassFiles(Iterables.getFirst(system.getRootDirectories(), null));
				}
			}
			ocp.addNonClassFiles(current, remapper, this.remappers);
			remapper.apply(ocp);
		} finally {
			remapper.finish();
		}
	}

	/**
	 * megamind moment
	 */
	@Override
	protected void copy() {
		super.copy();
		try {
			this.remap();
		} catch(IOException e) {
			throw U.rethrow(e);
		}
	}

	@Internal
	protected Path getCurrent() {
		return this.getArchiveFile().get().getAsFile().toPath();
	}
}
