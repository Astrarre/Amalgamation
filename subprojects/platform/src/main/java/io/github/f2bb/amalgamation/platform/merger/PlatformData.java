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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Information about the platform, such as its name(s) and files
 */
public class PlatformData {
	final Collection<String> name;
	final Map<String, byte[]> files;

	public PlatformData(Collection<String> name, Map<String, byte[]> files) {
		this.name = name;
		this.files = files;
	}

	public static void readFiles(Map<String, byte[]> files, Path root) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				files.put(root.relativize(file).toString(), Files.readAllBytes(file));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public String toString() {
		return "PlatformData{" +
				"name=" + name +
				'}';
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PlatformData)) {
			return false;
		}

		PlatformData data = (PlatformData) object;

		if (!Objects.equals(this.name, data.name)) {
			return false;
		}
		return Objects.equals(this.files, data.files);
	}

	@Override
	public int hashCode() {
		int result = this.name != null ? this.name.hashCode() : 0;
		result = 31 * result + (this.files != null ? this.files.hashCode() : 0);
		return result;
	}
}
