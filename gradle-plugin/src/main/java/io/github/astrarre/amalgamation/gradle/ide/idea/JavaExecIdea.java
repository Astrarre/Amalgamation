package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import io.github.astrarre.amalgamation.gradle.ide.util.CompressCmd;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;

public class JavaExecIdea extends FileTaskConverter<JavaExec> {
	// todo allow setting this globally for project
	// todo ease paralell compilation setup?

	/**
	 * Whether or not to build the project (with intellij/gradle) before running the task
	 */
	public boolean build = true;
	private String jvmVersion;
	private final Set<String> excludedDependencies = new HashSet<>();

	/**
	 * shorten the command line to use a manifest jar
	 */
	private CompressCmd shorten = CompressCmd.DEFAULT;

	private Project classpathProject;
	private SourceSet sourceSetClasspath;
	private CompressCmd.Idea ideaShorten;

	public JavaExecIdea(JavaExec task) {
		super(task);
	}

	/**
	 * If you are using the classpath of a source set, you can set this value and intellij will pull the classpath from there instead of us having to
	 * make our own manifest jar for the classpath
	 */
	public void overrideClasspath(Project project, SourceSet sourceSet, CompressCmd.Idea commandLine) {
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

	public void setShorten(String shorten) {
		this.setShorten(CompressCmd.valueOf(shorten));
	}

	public void setJvmVersion(String version) {
		this.jvmVersion = version;
	}

	public void excludeDependency(Task task) {
		this.excludedDependencies.add(task.getPath());
	}

	public static CompressCmd.Idea from(CompressCmd cmd) {
		return switch(cmd) {
			case MANIFEST_JAR -> CompressCmd.Idea.MANIFEST;
			case DEFAULT -> CompressCmd.Idea.NONE;
		};
	}

	@Override
	public void emit(boolean immediateEmission) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("<component name=\"ProjectRunConfigurationManager\">\n");

		builder.append("\t<configuration default=\"false\" ")
				.append("name=\"")
				.append(this.customName)
				.append('\"')
				.append(" type=\"Application\" factoryName=\"Application\">\n");

		if(this.sourceSetClasspath == null) {
			this.jvmVersion = System.getProperty("java.vm.specification.version");
		}
		if(this.jvmVersion != null) {
			builder.append("\t\t<option name=\"ALTERNATIVE_JRE_PATH\" value=\"").append(this.jvmVersion).append("\" />\n");
			builder.append("\t\t<option name=\"ALTERNATIVE_JRE_PATH_ENABLED\" value=\"true\" />\n");
		} else {
			builder.append("\t\t<option name=\"ALTERNATIVE_JRE_PATH_ENABLED\" value=\"false\" />\n");
		}

		// environment variables
		Map<String, String> map = this.task.getEnvironment()
				.entrySet()
				.stream()
				.map(e -> Map.entry(e.getKey(), e.getValue().toString()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		if((!(map instanceof HashMap))) {
			map = new HashMap<>(map); // ensure mutability
		}
		System.getenv().forEach(map::remove);

		if(!map.isEmpty()) {
			builder.append("\t\t<envs>\n");
			map.forEach((k, v) -> {
				builder.append("\t\t<env name=\"").append(k).append("\" value=\"").append(v).append("\">\n");
			});
			builder.append("\t\t</envs>\n");
		}

		// main class
		String cls = Objects.requireNonNull(this.task.getMainClass().getOrNull(), "Main-Class META-INF parsing is unsupported!");
		builder.append("\t\t<option name=\"MAIN_CLASS_NAME\" value=\"").append(cls).append("\" />\n");

		List<String> vmArgs = this.task.getAllJvmArgs();

		// classpath
		if(this.sourceSetClasspath != null) {
			builder.append("\t\t<module name=\"")
					.append(this.classpathProject.getName())
					.append(".")
					.append(this.sourceSetClasspath.getName())
					.append("\" />\n");

			builder.append("\t\t<shortenClasspath name=\"").append(this.ideaShorten).append("\" />\n");
		} else {
			vmArgs = appendClasspath(this.shorten, this.task, vmArgs);
		}

		// program args
		List<String> progArgs = this.task.getArgs();
		if(progArgs != null && !progArgs.isEmpty()) {
			builder.append("\t\t<option name=\"PROGRAM_PARAMETERS\" value=\"").append(String.join(" ", progArgs)).append("\" />\n");
		}

		// vm args
		if(!vmArgs.isEmpty()) {
			builder.append("\t\t<option name=\"VM_PARAMETERS\" value=\"").append(String.join(" ", vmArgs)).append("\" />\n");
		}

		// working directory
		String workingDir = this.getWorkingDirectory(this.task).replace("\\", "/");
		if(!workingDir.isBlank() && !workingDir.equals("/")) {
			builder.append("\t\t<option name=\"WORKING_DIRECTORY\" value=\"").append(workingDir).append("\" />\n");
		}

		// task dependencies
		Set<? extends Task> dependencies = this.task.getTaskDependencies().getDependencies(this.task);
		if(this.build || !dependencies.isEmpty()) {
			builder.append("\t\t<method v=\"2\">\n");
			if(this.build) {
				builder.append("\t\t\t<option name=\"Make\" enabled=\"true\" />\n");
			}
			if(!dependencies.isEmpty()) {
				for(Task dependency : dependencies) {
					if(excludedDependencies.contains(dependency.getPath())) {
						continue;
					}
					builder.append("\t\t\t<option name=\"Gradle.BeforeRunTask\" enabled=\"true\" tasks=\"").append(dependency.getPath())
							.append("\" externalProjectPath=\"$PROJECT_DIR$/").append(task.getProject().getPath().replace(':', '/'))
							.append("\" vmOptions=\"\" scriptParameters=\"\" />\n");
				}
			}
			builder.append("\t\t</method>\n");
		} else {
			builder.append("\t\t<method v=\"2\" />\n");
		}
		builder.append("\t</configuration>\n");
		builder.append("</component>\n");

		Path path = this.getProject().getRootDir().toPath().resolve(".idea").resolve("runConfigurations").resolve(this.customPath + ".xml");
		Files.createDirectories(path.getParent());
		try(BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write(builder.toString());
		}
	}

	public static List<String> appendClasspath(CompressCmd cmd, JavaExec exec, List<String> vmArgs) {
		vmArgs = new ArrayList<>(vmArgs);
		vmArgs.add("-cp");
		switch(cmd) {
			case DEFAULT -> {
				vmArgs.add(String.join(";", getClasspath(exec)));
			}
			case MANIFEST_JAR -> {
				File jar = getManifestJar(exec);
				vmArgs.add(jar.getAbsolutePath());
			}
		}
		return vmArgs;
	}

	public String getWorkingDirectory(JavaExec exec) {
		String workingDir = exec.getWorkingDir().getAbsolutePath();
		String projectRoot = this.getProject().getRootDir().getAbsolutePath();
		if(workingDir.startsWith(projectRoot)) {
			return "$PROJECT_DIR$" + workingDir.substring(projectRoot.length());
		} else {
			return workingDir;
		}
	}
}
