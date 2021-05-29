package io.github.astrarre.amalgamation.gradle.plugin.minecraft;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Set;

import com.google.gson.reflect.TypeToken;
import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.MinecraftDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.RemappingDependency;
import io.github.astrarre.amalgamation.gradle.files.CachedFile;
import io.github.astrarre.amalgamation.gradle.files.NativesFile;
import io.github.astrarre.amalgamation.gradle.files.assets.AssetProvider;
import io.github.astrarre.amalgamation.gradle.files.assets.Assets;
import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationImpl;
import io.github.astrarre.amalgamation.gradle.utils.Clock;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class MinecraftAmalgamationImpl extends BaseAmalgamationImpl implements MinecraftAmalgamation {
	public MinecraftAmalgamationImpl(Project project) {
		super(project);
	}

	@Override
	public Dependency client(String version) {
		return new MinecraftDependency(this.project, version, true, true, false);
	}

	@Override
	public Dependency server(String version) {
		return new MinecraftDependency(this.project, version, false, true, true);
	}

	@Override
	public Dependency libraries(String version, Action<LibrariesDependency> configure) {
		LibrariesDependency dependency = new LibrariesDependency(this.project, version);
		configure.execute(dependency);
		return dependency;
	}

	@Override
	public Assets assets(String version) {
		try(Clock clock = new Clock("Cache validation / download for assets took %sms", this.logger)) {
			return AssetProvider.getAssetsDir(this, version);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Type SET = new TypeToken<Set<String>>() {}.getType();

	@Override
	public String natives(String version) {
		LauncherMeta meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(this.project);
		CachedFile<Set<String>> natives = new NativesFile(this, version, meta, LauncherMeta.activeMinecraftDirectory() + "/libraries");
		return natives.getPath().toAbsolutePath().toString();
	}

	@Override
	public Dependency map(Action<RemappingDependency> mappings) {
		RemappingDependency dependency = new RemappingDependency(this.project);
		mappings.execute(dependency);
		return dependency;
	}

}