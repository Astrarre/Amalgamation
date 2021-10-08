package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.File;
import java.util.function.Consumer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

/**
 * Basic configuration
 */
public class ConfigIdea {
	public static void configure(Project project, Plugin<Project> ideaModel) {
		IdeaPlugin plugin = (IdeaPlugin) ideaModel;
		IdeaModule module = plugin.getModel().getModule();
		module.getExcludeDirs().addAll(project.files(".gradle", "build", ".idea", "out").getFiles());
		module.setDownloadJavadoc(true);
		module.setDownloadSources(true);
		module.setInheritOutputDirs(false);
		configureOutput(project, "compileJava", module::setOutputDir);
		configureOutput(project, "compileTestJava", module::setTestOutputDir);
	}

	public static void configureOutput(Project project, String taskName, Consumer<File> listen) {
		if(project.getTasks().findByName(taskName) instanceof AbstractCompile task) {
			listen.accept(task.getDestinationDir());
		} else {
			System.out.println("[Amalg] [Warn] Unable to find " + taskName);
		}
	}
}
