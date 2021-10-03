package io.github.astrarre.amalgamation.gradle.ide.intellij;

import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

public class ConfigureIntellij {
	public static void configure(Project project, IdeaModel ideaModel) {
		IdeaModule module = ideaModel.getModule();
		module.getExcludeDirs().addAll(project.files(".gradle", "build", ".idea", "out").getFiles());
		module.setDownloadJavadoc(true);
		module.setDownloadSources(true);
		module.setInheritOutputDirs(true);
	}
}
