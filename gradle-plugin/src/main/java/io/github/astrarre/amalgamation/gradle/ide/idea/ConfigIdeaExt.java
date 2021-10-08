package io.github.astrarre.amalgamation.gradle.ide.idea;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.reflect.TypeOf;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.RunConfiguration;
import org.jetbrains.gradle.ext.TaskTriggersConfig;

public class ConfigIdeaExt {
	public static IdeaExtension extension;
	public static void configure(Project project, Plugin<Project> plugin) {
		IdeaPlugin idea = (IdeaPlugin) plugin;
		if(idea.getModel().getProject() instanceof ExtensionAware e) {
			ProjectSettings settings = e.getExtensions().findByType(ProjectSettings.class);
			if(settings instanceof ExtensionAware ex) {
				NamedDomainObjectContainer<RunConfiguration> runCfg = ex.getExtensions().getByType(new TypeOf<>() {});
				PolymorphicDomainObjectContainer<RunConfiguration> container = (PolymorphicDomainObjectContainer<RunConfiguration>) runCfg;
				TaskTriggersConfig config = ex.getExtensions().getByType(TaskTriggersConfig.class);
				extension = new IdeaExtension(container);
				Task task = project.task("emitIdeaRunConfigs", t -> t.doFirst($ -> extension.configureQueue()));
				config.afterSync(task);
				return;
			}
		}
		extErr();
	}

	private static void extErr() {
		System.out.println("[Amalg] [Warn] idea-ext plugin not initialized?");
	}
}
