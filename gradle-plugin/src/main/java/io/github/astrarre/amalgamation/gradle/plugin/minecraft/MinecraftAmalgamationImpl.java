package io.github.astrarre.amalgamation.gradle.plugin.minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.astrarre.amalgamation.gradle.dependencies.CachedFileDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.RemappingDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.transforming.TransformingDependency;
import io.github.astrarre.amalgamation.gradle.files.CASMergedFile;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.MinecraftFileHelper;
import io.github.astrarre.amalgamation.gradle.files.MojMergedFile;
import io.github.astrarre.amalgamation.gradle.files.NativesFile;
import io.github.astrarre.amalgamation.gradle.files.assets.AssetProvider;
import io.github.astrarre.amalgamation.gradle.files.assets.Assets;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class MinecraftAmalgamationImpl extends BaseAmalgamationImpl implements MinecraftAmalgamation {
	public String librariesDirectory = LauncherMeta.activeMinecraftDirectory() + "/libraries";

	public MinecraftAmalgamationImpl(Project project) {
		super(project);
		File file = new File(this.librariesDirectory);
		if(!(file.isDirectory() && file.exists())) {
			this.librariesDirectory = AmalgIO.globalCache(project.getGradle()).resolve("libraries").toAbsolutePath().toString();
		}
	}


	@Override
	public Dependency client(String version, boolean split) {
		return this.mc(version, true, false, split);
	}

	@Override
	public Dependency server(String version, boolean strip, boolean split) {
		return this.mc(version, false, strip, split);
	}

	Dependency mc(String version, boolean isClient, boolean doStrip, boolean doSplit) {
		CachedFileDependency dependency = new CachedFileDependency(this.project, "net.minecraft", isClient ? "minecraft-client" : "minecraft-server", version);
		CachedFile file = MinecraftFileHelper.file(this.project, version, isClient, doStrip, doSplit);
		dependency.add(file);
		return dependency;
	}

	@Override
	public Dependency merged(String version, Action<CASMerger.Config> configurate) {
		CASMerger.Config config = new CASMerger.Config(this.project);
		config.version = version;
		configurate.execute(config);

		Path jar = AmalgIO.globalCache(this.project.getGradle()).resolve(version).resolve("merged.jar");
		CASMergedFile file = new CASMergedFile(jar, this.project, version, config.handler, config.clsReaderFlags, config.checkForServerOnly, config.server, config.client);
		CachedFileDependency dependency = new CachedFileDependency(this.project, "net.minecraft", "moj-merged", version);
		dependency.add(file);
		return dependency;
	}

	@Override
	public Dependency mojmerged(String version, CASMerger.Handler handler, boolean split) {
		CachedFile file = MinecraftFileHelper.mojmap(this.project, version, false);
		Path jar = AmalgIO.globalCache(this.project.getGradle()).resolve(version).resolve("moj-merged.jar");
		var moj = new MojMergedFile(jar, this.project, this.client(version, split), version, handler, file);
		CachedFileDependency dependency = new CachedFileDependency(this.project, "net.minecraft", "moj-merged", version);
		dependency.add(moj);
		return dependency;
	}

	@Override
	public List<Dependency> fabricLoader(String version) {
		List<Dependency> dependencies = new ArrayList<>();
		DependencyHandler handler = this.project.getDependencies();
		dependencies.add(handler.create("net.fabricmc:fabric-loader:" + version));
		File json = AmalgIO.resolve(this.project, "net.fabricmc:fabric-loader:" + version + "@json");
		try(BufferedReader reader = Files.newBufferedReader(json.toPath())) {
			JsonObject object = CachedFile.GSON.fromJson(reader, JsonObject.class);
			JsonObject libs = object.getAsJsonObject("libraries");
			for(JsonElement common : libs.getAsJsonArray("common")) {
				JsonObject dep = common.getAsJsonObject();
				String depL = dep.get("name").getAsString();
				dependencies.add(handler.create(depL));
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return dependencies;
	}

	@Override
	public Dependency transformed(String name, Action<TransformingDependency> transformer) {
		TransformingDependency dep = new TransformingDependency(this.project, name);
		transformer.execute(dep);
		return dep;
	}

	@Override
	public Dependency accessWidener(String name, Dependency dependency, Object accessWidener) {
		return this.transformed(name, d -> {
			try {
				d.accessWidener(accessWidener);
				d.transform(dependency);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public LibrariesDependency libraries(String version, Action<LibrariesDependency> configure) {
		LibrariesDependency dependency = new LibrariesDependency(this.project, version);
		configure.execute(dependency);
		return dependency;
	}

	@Override
	public Assets assets(String version) {
		try(Clock clock = new Clock("Cache validation / download for assets took %sms", this.logger)) {
			return AssetProvider.getAssetsDir(this, version);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String natives(String version) {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		CachedFile natives = new NativesFile(this, version, meta);
		return natives.getOutput().toAbsolutePath().toString();
	}

	@Override
	public Dependency map(Action<RemappingDependency> mappings) {
		RemappingDependency dependency = new RemappingDependency(this.project);
		mappings.execute(dependency);
		return dependency;
	}

	@Override
	public void setLibrariesCache(String directory) {
		this.librariesDirectory = directory;
	}

	@Override
	public String librariesCache() {
		return this.librariesDirectory;
	}
}
