package io.github.astrarre.amalgamation.gradle.dependencies.remap.api;

import java.util.List;

import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;

public interface ZipRemapper {
	/**
	 * visits an entry in a zip file
	 * @param isClasspath if the entry is from a jar marked as classpath
	 * @return true if the entry was handled (if it was either deleted, will be remapped, or was already remapped)
	 */
	boolean visitEntry(VirtualZipEntry entry, boolean isClasspath);

	default void acceptPost(ZipOutput output) {}

	record Combined(List<ZipRemapper> remappers) implements ZipRemapper {
		@Override
		public boolean visitEntry(VirtualZipEntry entry, boolean isClasspath) {
			for(ZipRemapper remapper : this.remappers) {
				if(remapper.visitEntry(entry, isClasspath)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void acceptPost(ZipOutput output) {
			for(ZipRemapper remapper : this.remappers) {
				remapper.acceptPost(output);
			}
		}
	}
}
