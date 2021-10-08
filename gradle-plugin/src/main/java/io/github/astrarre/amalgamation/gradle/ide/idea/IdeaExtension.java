package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.util.ArrayList;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.ide.TaskConverter;
import org.gradle.api.Action;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.jetbrains.gradle.ext.RunConfiguration;

public class IdeaExtension {
	final PolymorphicDomainObjectContainer<RunConfiguration> ideaRunConfigFactory;

	List<TaskConverter<?>> converters = new ArrayList<>();

	public IdeaExtension(PolymorphicDomainObjectContainer<RunConfiguration> factory) {
		this.ideaRunConfigFactory = factory;
	}

	public void java(JavaExec exec, Action<JavaExecIdea> config) {
		JavaExecIdea idea = new JavaExecIdea(exec, this.ideaRunConfigFactory);
		this.converters.add(idea);
		config.execute(idea);
	}

	public void exec(Task exec, Action<TaskIdea> config) {
		TaskIdea idea = new TaskIdea(exec, this.ideaRunConfigFactory);
		this.converters.add(idea);
		config.execute(idea);
	}

	public void configureQueue() {
		for(TaskConverter<?> value : this.converters) {
			value.emit();
		}
	}
}
