package io.github.astrarre.amalgamation.gradle.plugin.minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.AccessWidenerDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.AssetsDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.CASMergedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.RemapDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.RemapDependencyConfig;
import io.github.astrarre.amalgamation.gradle.dependencies.util.MinecraftFileHelper;
import io.github.astrarre.amalgamation.gradle.dependencies.mojmerge.MojMergedDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.NativesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class MinecraftAmalgamationImpl extends BaseAmalgamationImpl implements MinecraftAmalgamation {
	private String librariesDirectory = LauncherMeta.activeMinecraftDirectory() + "/libraries";

	public MinecraftAmalgamationImpl(Project project) {
		super(project);
		File file = new File(this.librariesDirectory);
		if(!(file.isDirectory() && file.exists())) {
			this.librariesDirectory = AmalgIO.globalCache(project).resolve("libraries").toAbsolutePath().toString();
		}
	}

	@Override
	public Object client(String version, boolean split) {
		return this.mc(version, true, false, split);
	}

	@Override
	public Object server(String version, boolean strip, boolean split) {
		return this.mc(version, false, strip, split);
	}

	Object mc(String version, boolean isClient, boolean doStrip, boolean doSplit) {
		return MinecraftFileHelper.getDependency(project, version, isClient, doStrip, doSplit);
	}

	@Override
	public Object merged(String version, Action<CASMergedDependency> configurate) {
		CASMergedDependency dependency = new CASMergedDependency(this.project, version);
		configurate.execute(dependency);
		return dependency;
	}

	@Override
	public Object mojmerged(String version, SideAnnotationHandler handler, boolean split, MappingTarget dependency) {
		return new MojMergedDependency(this.project, version, handler, this.client(version, split), dependency);
	}

	@Override
	public List<Dependency> fabricLoader(String version) {
		List<Dependency> dependencies = new ArrayList<>();
		DependencyHandler handler = this.project.getDependencies();
		dependencies.add(handler.create("net.fabricmc:fabric-loader:" + version));
		File json = AmalgIO.resolveFile(this.project, "net.fabricmc:fabric-loader:" + version + "@json");
		try(BufferedReader reader = Files.newBufferedReader(json.toPath())) {
			JsonObject object = LauncherMeta.GSON.fromJson(reader, JsonObject.class);
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
	public Object libraries(String version, Action<LibrariesDependency> configure) {
		LibrariesDependency dependency = new LibrariesDependency(this.project, version);
		configure.execute(dependency);
		return dependency;
	}

	@Override
	public AssetsDependency assets(String version) {
		return new AssetsDependency(this, version);
	}

	@Override
	public String natives(String version) {
		return new NativesDependency(this.project, version).getNativesDirectory();
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
