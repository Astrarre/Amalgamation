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

package io.github.astrarre.amalgamation.gradle.tasks;

import java.io.File;
import java.io.IOException;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class RemapJar extends Jar {
	private FileCollection classpath;
	private FileCollection mappings;

	@InputFiles
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	@InputFiles
	public FileCollection getMappings() {
		return this.mappings;
	}


	@TaskAction
	public void remap() throws IOException {
		TinyRemapper remapper = TinyRemapper.newRemapper()
		                                    //todo .withMappings(MappingUtils.createMappingProvider(mappings))
		                                    .build();

		FileCollection inputs = this.getInputs().getFiles();

		for (File file : inputs) {
			remapper.readInputsAsync(file.toPath());
		}

		for (File file : this.classpath) {
			remapper.readClassPathAsync(file.toPath());
		}

		try (OutputConsumerPath outputConsumerPath = new OutputConsumerPath.Builder(this.getArchiveFile().get().getAsFile().toPath()).build()) {
			for (File file : inputs) {
				outputConsumerPath.addNonClassFiles(file.toPath());
			}

			remapper.apply(outputConsumerPath);
		} finally {
			remapper.finish();
		}
	}

}
