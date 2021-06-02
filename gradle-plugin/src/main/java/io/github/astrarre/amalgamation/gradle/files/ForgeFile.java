package io.github.astrarre.amalgamation.gradle.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Supplier;

import io.github.astrarre.amalgamation.gradle.forge.ForgeUtil;
import org.jetbrains.annotations.Nullable;

public class ForgeFile extends CachedFile<String> {
	public static final String MINECRAFT_VERSION;
	public static final String FORGE_VERSION;

	static {
		try {
			Properties properties = new Properties();
			properties.load(ForgeUtil.class.getResourceAsStream("/gradle_data.properties"));
			MINECRAFT_VERSION = properties.getProperty("minecraft_version");
			FORGE_VERSION = properties.getProperty("forge_version");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final boolean client;
	public final File minecraft;

	public ForgeFile(Path file, boolean client, File minecraft) {
		super(file, String.class);
		this.client = client;
		this.minecraft = minecraft;
	}

	public ForgeFile(Supplier<Path> file, boolean client, File minecraft) {
		super(file, String.class);
		this.client = client;
		this.minecraft = minecraft;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {

		return null;
	}
}
