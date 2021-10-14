package io.github.astrarre.amalgamation.gradle.dependencies;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.forge.ForgeUtil;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class ForgeDependency extends CachedDependency {
	public final Dependency minecraft;
	public final boolean isClient;
	public final LibrariesDependency libraries;
	public final String forgeVersion;

	public ForgeDependency(Project project,
			String group,
			String name,
			String version,
			Dependency minecraft,
			boolean client,
			LibrariesDependency libraries,
			String forgeVersion) {
		super(project, group, name, version);
		this.minecraft = minecraft;
		this.isClient = client;
		this.libraries = libraries;
		this.forgeVersion = forgeVersion;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		hasher.putString(this.forgeVersion, StandardCharsets.UTF_8);

	}

	@Override
	protected Path evaluatePath(byte[] hash) throws MalformedURLException {
		String vers = minecraft.getVersion() + "-" + forgeVersion;
		return Paths.get(
				libraries.librariesDirectory,
				"net",
				"minecraftforge",
				"forge",
				vers,
				isClient ? "forge-" + vers + "-client.jar" : "forge-" + vers + "-server.jar");
	}

	@Override
	protected Iterable<Path> resolve0(Path resolvedPath, boolean isOutdated) throws IOException {
		File file = AmalgIO.resolve(this.libraries.project, this.minecraft);
		File libs = new File(this.libraries.librariesDirectory);
		if(this.isClient) {
			ForgeUtil.getForgeClient(libs, file);
		} else {
			ForgeUtil.getForgeServer(libs, file);
		}
		return List.of(resolvedPath);
	}
}
