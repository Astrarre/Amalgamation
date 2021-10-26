package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyArtifactSelector;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.Dependency;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.plugins.ide.idea.model.Module;
import org.gradle.plugins.ide.idea.model.ModuleLibrary;

/**
 * Basic configuration
 */
public class ConfigIdea {
	public static void configure(Project project, Plugin<Project> ideaModel) {
		// todo for sources
		// step 0: hash file name without the classifier (robust solution is easy)
		// step 1: put hash as part of file name (before classifier / extension)
		// step 2: add directories as flat directories
		// step 3: make all multi-process dependencies return a list of Dependency


		//project.getDependencies().create()

		IdeaPlugin plugin = (IdeaPlugin) ideaModel;

		plugin.getModel().module(m -> {
			m.iml(i -> {
				i.setGenerateTo(new File("testaaaaa"));
			});
		});
		project.afterEvaluate(project1 -> {
			plugin.getModel().getModule().iml(iml -> {
				iml.whenMerged(o -> {
					Module module = (Module) o;
					module.getDependencies().clear();
				});
			});
			System.out.println(plugin.getModel().getProject().getProjectLibraries());
			System.out.println(plugin.getModel().getModule().resolveDependencies());
			for(Dependency dependency : plugin.getModel().getModule().resolveDependencies()) {
				if(dependency instanceof ModuleLibrary lib) {
					var iterator = lib.getClasses().iterator();
					while(iterator.hasNext()) {
						var cls = iterator.next();
						try {
							String url = cls.getCanonicalUrl();
							if(url.startsWith("jar://")) {
								url = url.substring("jar://".length());
							}

							if(url.endsWith("!/")) {
								url = url.substring(0, url.length() - 2);
							}

							Path path = Path.of(url).toRealPath();
							if(AmalgIO.SOURCES.contains(path)) {
								iterator.remove();
								lib.getSources().add(new org.gradle.plugins.ide.idea.model.Path(path.toFile().getAbsolutePath()));
							}
						} catch(IOException e) {
							throw U.rethrow(e);
						}
					}
				}
			}
		});
		IdeaModule module = plugin.getModel().getModule();
		module.getExcludeDirs().addAll(project.files(".gradle", "build", ".idea", "out").getFiles());
		module.setDownloadJavadoc(true);
		module.setDownloadSources(true);
		module.setInheritOutputDirs(false);
		project.getPlugins().withId("java", $ -> {
			configureOutput(project, "compileJava", module::setOutputDir);
			configureOutput(project, "compileTestJava", module::setTestOutputDir);
		});
	}

	public static void configureOutput(Project project, String taskName, Consumer<File> listen) {
		if(project.getTasks().findByName(taskName) instanceof AbstractCompile task) {
			listen.accept(task.getDestinationDir());
		} else {
			System.out.println("[Amalg] [Warn] Unable to find " + taskName);
		}
	}
}
