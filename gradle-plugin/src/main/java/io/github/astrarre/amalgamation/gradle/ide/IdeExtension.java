package io.github.astrarre.amalgamation.gradle.ide;

import java.io.IOException;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.ide.intellij.TaskToIntellij;
import org.gradle.api.Action;
import org.gradle.api.Task;

public class IdeExtension {
	public TaskToIntellij intellij(Task task, Action<TaskToIntellij> intellij) throws IOException {
		TaskToIntellij ij = new TaskToIntellij(task);
		intellij.execute(ij);
		ij.emit();
		return ij;
	}
}
