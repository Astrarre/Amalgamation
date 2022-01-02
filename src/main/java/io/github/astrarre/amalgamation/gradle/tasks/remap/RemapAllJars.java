package io.github.astrarre.amalgamation.gradle.tasks.remap;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class RemapAllJars extends DefaultTask {

	@Input
	abstract ListProperty<RemapJar> getInputTasks();

	@TaskAction
	public void run() {

		for(RemapJar task : getInputTasks().get()) {

		}
	}
}
