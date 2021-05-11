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

package io.github.f2bb.amalgamation.platform.merger;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Information about the platform, such as its name(s) and files
 */
public class PlatformData implements Closeable {
	public final List<String> name;
	public final List<Path> paths;
	private Closeable current;

	public PlatformData(List<String> name, List<Path> paths) {
		this.name = name;
		this.paths = paths;
	}

	public void addCloseAction(Closeable closeable) {
		if (this.current == null) {
			this.current = closeable;
		} else {
			Closeable current = this.current;
			this.current = () -> {
				current.close();
				closeable.close();
			};
		}
	}

	public byte[] get(String string) {
		try {
			for (Path path : this.paths) {
				Path resolved = path.resolve(string);
				if (Files.exists(resolved)) {
					return Files.readAllBytes(resolved);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException(new FileNotFoundException(string));
	}

	public void forEach(BiConsumer<String, Path> files) throws IOException {
		for (Path path : paths) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					files.accept(path.relativize(file).toString(), file);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	@Override
	public String toString() {
		return "PlatformData{" + "name=" + name + '}';
	}

	@Override
	public void close() throws IOException {
		this.current.close();
	}
}
