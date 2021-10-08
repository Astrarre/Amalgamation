package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import io.github.astrarre.amalgamation.gradle.ide.NamedTaskConverter;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.Task;
import org.jetbrains.gradle.ext.Gradle;
import org.jetbrains.gradle.ext.GradleTask;
import org.jetbrains.gradle.ext.RunConfiguration;

public class TaskIdea extends NamedTaskConverter<Task> {
	final PolymorphicDomainObjectContainer<RunConfiguration> runCfgFactory;
	final Map<String, String> envs = new HashMap<>();
	final List<String> jvmArgs = new ArrayList<>();
	final List<String> scriptParameters = new ArrayList<>();

	public TaskIdea(Task task, PolymorphicDomainObjectContainer<RunConfiguration> factory) {
		super(task);
		this.runCfgFactory = factory;
	}

	public void addEnv(String key, Object val) {
		this.envs.put(key, val.toString());
	}

	public void addArg(String jvmArg) {
		this.jvmArgs.add(jvmArg);
	}

	public void addScriptParam(String param) {
		this.scriptParameters.add(param);
	}

	@Override
	public void emit() {
		Gradle gradle = this.runCfgFactory.create(this.customName, Gradle.class);
		gradle.setProject(this.task.getProject());
		gradle.setTaskNames(List.of(this.task.getPath()));
		gradle.setEnvs(this.envs);
		gradle.setJvmArgs(String.join(" ", this.jvmArgs));
		gradle.setScriptParameters(String.join(" ", this.scriptParameters));
	}
}
