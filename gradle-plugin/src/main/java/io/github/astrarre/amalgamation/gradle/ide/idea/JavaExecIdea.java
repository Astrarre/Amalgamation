package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.astrarre.amalgamation.gradle.ide.NamedTaskConverter;
import io.github.astrarre.amalgamation.gradle.ide.util.CompressCmd;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.tasks.TaskDependencyInternal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.gradle.ext.Application;
import org.jetbrains.gradle.ext.GradleTask;
import org.jetbrains.gradle.ext.Make;
import org.jetbrains.gradle.ext.RunConfiguration;
import org.jetbrains.gradle.ext.ShortenCommandLine;

public class JavaExecIdea extends NamedTaskConverter<JavaExec> {
	// todo allow setting this globally for project
	// todo ease paralell compilation setup?

	final PolymorphicDomainObjectContainer<RunConfiguration> runCfgFactory;
	/**
	 * Whether or not to build the project (with intellij/gradle) before running the task
	 */
	public boolean build = true;

	/**
	 * shorten the command line to use a manifest jar
	 */
	private CompressCmd shorten = CompressCmd.NONE;

	private Project classpathProject;
	private SourceSet sourceSetClasspath;
	private ShortenCommandLine ideaShorten;

	public JavaExecIdea(JavaExec task, PolymorphicDomainObjectContainer<RunConfiguration> factory) {
		super(task);
		this.runCfgFactory = factory;
	}

	/**
	 * If you are using the classpath of a source set, you can set this value and intellij will pull the classpath from there instead of us having
	 *  to make our own manifest jar for the classpath
	 */
	public void overrideClasspath(Project project, SourceSet sourceSet, ShortenCommandLine commandLine) {
		this.classpathProject = Objects.requireNonNull(project, "project cannot be null");
		this.sourceSetClasspath = Objects.requireNonNull(sourceSet, "sourceSet cannot be null");
		this.ideaShorten = Objects.requireNonNull(commandLine, "command line compressor cannot be null");
	}

	public void overrideClasspath(Project project, SourceSet sourceSet) {
		this.overrideClasspath(project, sourceSet, from(this.shorten));
	}

	public void setShorten(CompressCmd shorten) {
		if(this.classpathProject != null) {
			this.ideaShorten = from(shorten);
		}
		this.shorten = shorten;
	}

	public static ShortenCommandLine from(CompressCmd cmd) {
		return switch(cmd) {
			case MANIFEST_JAR -> ShortenCommandLine.MANIFEST;
			case NONE -> ShortenCommandLine.NONE;
		};
	}

	@Override
	public void emit() {
		Application application = this.runCfgFactory.create(this.customName, Application.class);

		// environment variables
		var map = new HashMap<>(this.task.getEnvironment());
		System.getenv().forEach(map::remove);
		for(var entry : map.entrySet()) {
			entry.setValue(entry.getValue().toString());
		}
		application.setEnvs((Map) map);

		List<String> vmArgs = this.task.getAllJvmArgs();

		// classpath
		if(this.sourceSetClasspath != null) {
			application.moduleRef(this.classpathProject, this.sourceSetClasspath);
			application.setShortenCommandLine(this.ideaShorten);
		} else {
			vmArgs = new ArrayList<>(vmArgs);
			vmArgs.add("-cp");
			switch(this.shorten) {
				case NONE -> {
					vmArgs.add(String.join(";", getClasspath(this.task)));
				}
				case MANIFEST_JAR -> {
					File jar = this.getManifestJar(this.task);
					vmArgs.add(jar.getAbsolutePath());
				}
			}
		}

		// main class
		String cls = Objects.requireNonNull(this.task.getMainClass().getOrNull(), "Main-Class parsing is unsupported!");
		application.setMainClass(cls);

		// working directory
		application.setWorkingDirectory(this.task.getWorkingDir().getAbsolutePath());

		// vm args
		if(!vmArgs.isEmpty()) {
			application.setJvmArgs(String.join(" ", vmArgs));
		}

		// program args
		List<String> progArgs = this.task.getArgs();
		if (progArgs != null && !progArgs.isEmpty()) {
			application.setProgramParameters(String.join(" ", vmArgs));
		}

		// task dependencies
		application.beforeRun(tasks -> {
			if(this.build) {
				tasks.create("build", Make.class);
			}
			TaskDependencyInternal dependencies = this.task.getTaskDependencies();
			for(Task dependency : dependencies.getDependencies(this.task)) {
				GradleTask task = tasks.create(dependency.getPath(), GradleTask.class);
				task.setTask(dependency);
			}
		});
	}
}
