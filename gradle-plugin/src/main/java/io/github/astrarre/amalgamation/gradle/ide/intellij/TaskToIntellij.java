package io.github.astrarre.amalgamation.gradle.ide.intellij;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.Task;

public class TaskToIntellij extends IntellijTaskConverter<Task> {

	public TaskToIntellij(Task task) {
		super(task);
	}

	@Override
	public void emit() throws IOException {
		Project project = this.task.getProject();
		Path dir = getIdeaDir(project);
		if(!Files.exists(dir)) {
			return;
		}
		Map<String, String> parameters = new HashMap<>();
		parameters.put("%AMALG.INTELLIJ_NAME%", this.customName);
		parameters.put("%AMALG.PROJECT_DIR%", "$PROJECT_DIR$" + pathExtension(project));
		parameters.put("%AMALG.TASK_NAME%", this.task.getName());
		writeTemplate(dir.resolve("runConfigurations").resolve(this.customPath + ".xml"), "/templates/intellij_gradle_task.xml", parameters);
	}
	// externalProjectPath = "$PROJECT_DIR$" for root
	// but for subprojects it's "$PROJECT_DIR$/relativePath" eg "$PROJECT_DIR$/test"
}
