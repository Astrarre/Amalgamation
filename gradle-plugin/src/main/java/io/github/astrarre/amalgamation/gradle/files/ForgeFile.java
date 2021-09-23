package io.github.astrarre.amalgamation.gradle.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.MinecraftDependency;
import io.github.astrarre.amalgamation.gradle.forge.ForgeUtil;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
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
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final boolean client;
	public final MinecraftDependency minecraft;
	public final LibrariesDependency libraries;

	public ForgeFile(boolean client, MinecraftDependency minecraft, LibrariesDependency libraries, String forgeVersion) {
		super(() -> {
			String vers = minecraft.getVersion() + "-" + forgeVersion;
			return Paths.get(libraries.librariesDirectory,
			                 "net",
			                 "minecraftforge",
			                 "forge",
			                 vers,
			                 client ? "forge-" + vers + "-client.jar" : "forge-" + vers + "-server.jar");
		}, String.class);
		this.client = client;
		this.minecraft = minecraft;
		this.libraries = libraries;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		String str = AmalgIO.hash(this.minecraft.resolve());
		if(!str.equals(currentData)) {
			if(this.minecraft.isClient) {
				ForgeUtil.getForgeClient(new File(this.libraries.librariesDirectory),
				                         Iterables.getFirst(this.minecraft.resolve(), null));
			} else {
				ForgeUtil.getForgeServer(new File(this.libraries.librariesDirectory),
				                         Iterables.getFirst(this.minecraft.resolve(), null));
			}
		}
		return null;
	}
}
