package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.File;
import java.util.function.Consumer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

/**
 * Basic configuration
 */
public class ConfigIdea {
	public static final IdeaExtension IDEA_EXTENSION = new IdeaExtension();
	public static void configure(Project project, Plugin<Project> ideaModel) {
		// todo for sources
		// step 1: put hash as part of file name (before classifier / extension)
		// step 2: add directories as flat directories
		// step 3: make all multi-process dependencies return a list of Dependency

		//project.getDependencies().create()

		IdeaPlugin plugin = (IdeaPlugin) ideaModel;
		IdeaModule module = plugin.getModel().getModule();
		module.getExcludeDirs().addAll(project.files(".gradle", "build", ".idea", "out").getFiles());
		module.setDownloadJavadoc(true);
		module.setDownloadSources(true);
		module.setInheritOutputDirs(false);

		project.getPlugins().withId("java", $ -> {
			// todo idk
			for(Project allproject : project.getAllprojects()) {
				configureOutput(allproject, "compileJava", module::setOutputDir);
				configureOutput(allproject, "compileTestJava", module::setTestOutputDir);
			}
		});

		/*if(Boolean.getBoolean("idea.sync.active")) {
			project.getRootProject().afterEvaluate(project1 -> {
				IDEA_EXTENSION.configureQueue(false);
			}); // todo fix
		}*/
	}

	public static void configureOutput(Project project, String taskName, Consumer<File> listen) {
		if(project.getTasks().findByName(taskName) instanceof AbstractCompile task) {
			listen.accept(task.getDestinationDir());
		} else {
			System.out.println("[Amalg] [Warn] Unable to find " + taskName);
		}
	}
}
