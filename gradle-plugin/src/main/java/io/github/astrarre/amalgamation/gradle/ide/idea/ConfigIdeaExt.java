package io.github.astrarre.amalgamation.gradle.ide.idea;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.reflect.TypeOf;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.RunConfiguration;

public class ConfigIdeaExt {
	public static IdeaExtension extension;
	public static void configure(Project project, IdeaPlugin plugin) {
		if(plugin.getModel().getProject() instanceof ExtensionAware e) {
			ProjectSettings settings = e.getExtensions().findByType(ProjectSettings.class);
			if(settings instanceof ExtensionAware ex) {
				NamedDomainObjectContainer<RunConfiguration> runCfg = ex.getExtensions().getByType(new TypeOf<>() {});
				PolymorphicDomainObjectContainer<RunConfiguration> container = (PolymorphicDomainObjectContainer<RunConfiguration>) runCfg;
				extension = new IdeaExtension(container);
				return;
			}
		}
		extErr();
	}

	private static void extErr() {
		System.out.println("[Amalg] [Warn] idea-ext plugin not initialized?");
	}
}
