package io.github.astrarre.amalgamation.gradle.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.URLCachedFile;
import io.github.astrarre.amalgamation.gradle.merger.Mergers;
import org.gradle.api.logging.Logger;

public class MinecraftFile extends URLCachedFile.Hashed {
	public final boolean isClient, doStrip;
	public MinecraftFile(Path file, LauncherMeta.HashedURL url, Logger logger, boolean compress, boolean isClient, boolean doStrip) {
		super(file, url, logger, compress);
		this.isClient = isClient;
		this.doStrip = doStrip;
	}

	@Override
	protected InputStream process(InputStream input, Path file) throws IOException, URISyntaxException {
		Files.createDirectories(file);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input));
		Path classesJar = file.resolve("classes.jar");
		Path resourcesJar = file.resolve("resources.jar");
		try(ZipOutputStream classes = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(classesJar))); ZipOutputStream resources = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(resourcesJar)))) {
			resources.putNextEntry(new ZipEntry(Mergers.RESOURCES_MARKER_FILE));
			resources.closeEntry();
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if(entry.isDirectory()) {
					resources.putNextEntry(entry);
					resources.closeEntry();
				} else {
					String name = entry.getName();
					ZipOutputStream toWriteTo;
					if(name.endsWith(".class")) {
						if(this.doStrip && !name.startsWith("net/minecraft/") && name.contains("/")) {
							toWriteTo = null;
						} else {
							toWriteTo = classes;
						}
					} else {
						toWriteTo = resources;
					}

					if(toWriteTo != null) {
						toWriteTo.putNextEntry(entry);
						Mergers.copy(zis, toWriteTo);
						toWriteTo.closeEntry();
					}
				}
				//zis.closeEntry();
			}
		}
		return zis;
	}


}
