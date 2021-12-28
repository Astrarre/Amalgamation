package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.ide.TaskConverter;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;

public class IdeaExtension {
	List<TaskConverter<?>> converters = new ArrayList<>();

	public void java(JavaExec exec, Action<JavaExecIdea> config) {
		JavaExecIdea idea = new JavaExecIdea(exec);
		this.converters.add(idea);
		config.execute(idea);
	}

	public void exec(Task exec, Action<TaskIdea> config) {
		TaskIdea idea = new TaskIdea(exec);
		this.converters.add(idea);
		config.execute(idea);
	}

	void configureQueue(boolean isImmediate) {
		for(TaskConverter<?> value : this.converters) {
			try {
				value.emit(isImmediate);
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
	}
}
