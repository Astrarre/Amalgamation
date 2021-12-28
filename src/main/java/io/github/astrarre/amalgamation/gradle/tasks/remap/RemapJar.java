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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.astrarre.amalgamation.gradle.tasks.remap.remap.AwResourceRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.jvm.tasks.Jar;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

public abstract class RemapJar extends Jar implements RemapTask {
	final List<OutputConsumerPath.ResourceRemapper> remappers = new ArrayList<>();

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

	public void remap() throws IOException {
		FileCollection classpath = this.getClasspath().get();
		IMappingProvider from = Mappings.from(this.readMappings());
		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(from).build();

		Path current = this.getCurrent();

		List<CompletableFuture<?>> futures = new ArrayList<>();
		futures.add(remapper.readInputsAsync(current));

		for(File file : classpath) {
			futures.add(remapper.readClassPathAsync(file.toPath()));
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		try(OutputConsumerPath ocp = new OutputConsumerPath.Builder(current).build()) {
			ocp.addNonClassFiles(current, remapper, this.remappers);
			remapper.apply(ocp);
		} finally {
			remapper.finish();
		}
	}
}
