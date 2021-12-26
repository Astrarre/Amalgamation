package io.github.astrarre.amalgamation.gradle.ide.idea;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import org.gradle.api.Task;

public class TaskIdea extends FileTaskConverter<Task> {
	final Map<String, String> envs = new HashMap<>();
	final List<String> jvmArgs = new ArrayList<>();
	final List<String> scriptParameters = new ArrayList<>();

	public TaskIdea(Task task) {
		super(task);
		this.scriptParameters.add(task.getPath());
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
	public void emit(boolean immediateEmission) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("<component name=\"ProjectRunConfigurationManager\">\n");

		builder.append("\t<configuration default=\"false\" ")
				.append("name=\"").append(this.customName).append('\"')
				.append(" type=\"GradleRunConfiguration\" factoryName=\"Gradle\">\n");

		builder.append("\t\t<ExternalSystemSettings>\n");
		if(!this.envs.isEmpty()) {
			builder.append("\t\t\t<option name=\"env\">\n");
			builder.append("\t\t\t\t<map>\n");
			this.envs.forEach((k, v) -> {
				builder.append("\t\t\t\t\t<entry key=\"").append(k).append("\" value=\"").append(v).append("\" />\n");
			});
			builder.append("\t\t\t\t</map>\n");
			builder.append("\t\t\t</option>\n");
		}
		builder.append("\t\t\t<option name=\"executionName\" />\n");
		builder.append("\t\t\t<option name=\"externalProjectPath\" value=\"").append(getProject().getProjectDir().getAbsolutePath()).append("\" />\n");
		builder.append("\t\t\t<option name=\"externalSystemIdString\" value=\"GRADLE\" />\n");
		builder.append("\t\t\t<option name=\"scriptParameters\" value=\"").append(String.join(" ", this.scriptParameters)).append("\" />\n");
		builder.append("\t\t\t<option name=\"taskDescriptions\"><list /></option>\n");
		builder.append("\t\t\t<option name=\"taskNames\"><list /></option>\n");
		builder.append("\t\t\t<option name=\"vmOptions\" value=\"").append(String.join(" ", this.jvmArgs)).append("\" />\n");
		builder.append("\t\t</ExternalSystemSettings>\n");
		builder.append("\t\t<ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>\n");
		builder.append("\t\t<ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>\n");
		builder.append("\t\t<DebugAllEnabled>false</DebugAllEnabled>\n");
		builder.append("\t\t<method v=\"2\" />\n");
		builder.append("\t</configuration>\n");
		builder.append("</component>\n");

		Path path = this.getProject().getRootDir().toPath().resolve(".idea").resolve("runConfigurations").resolve(this.customPath + ".xml");
		Files.createDirectories(path.getParent());
		try(BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write(builder.toString());
		}
	}
}
