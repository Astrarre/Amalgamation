package io.github.astrarre.amalgamation.gradle.ide.eclipse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.astrarre.amalgamation.gradle.ide.FileTaskConverter;
import io.github.astrarre.amalgamation.gradle.ide.idea.JavaExecIdea;
import io.github.astrarre.amalgamation.gradle.ide.util.CompressCmd;
import org.gradle.api.tasks.JavaExec;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.EclipseProject;

public class JavaExecEclipse extends FileTaskConverter<JavaExec> {

	/**
	 * shorten the command line to use a manifest jar
	 */
	private CompressCmd shorten = CompressCmd.DEFAULT;

	public JavaExecEclipse(JavaExec task) {
		super(task);
	}

	public void setShorten(CompressCmd shorten) {
		this.shorten = shorten;
	}

	@Override
	public void setCustomName(String customName) {
		EclipseModel model = this.getProject().getExtensions().getByType(EclipseModel.class);
		super.setCustomName(customName);
		this.setCustomPath(model.getProject().getName() + "_" + this.getCustomPath());
	}

	@Override
	public void emit(boolean immediateEmission) throws IOException {
		Map<String, String> params = new HashMap<>();
		params.put("%MAIN_CLASS%", Objects.requireNonNull(this.task.getMain(), "Main-Class autodetection is unsupported! Must set main in task!"));
		List<String> progArgs = Objects.requireNonNullElse(this.task.getArgs(), List.of());
		params.put("%PROGRAM_ARGS%", String.join(" ", progArgs));
		EclipseModel model = this.task.getProject().getExtensions().getByType(EclipseModel.class);
		EclipseProject project = model.getProject();
		params.put("%ECLIPSE_PROJECT%", project.getName());

		List<String> vmArgs = this.task.getAllJvmArgs();
		vmArgs = JavaExecIdea.appendClasspath(this.shorten, this.task, vmArgs);

		params.put("%VM_ARGS%", String.join(" ", vmArgs));
		params.put("%RUN_DIRECTORY%", this.task.getWorkingDir().getAbsolutePath());

		Path cfgFile = this.getProject().getProjectDir().toPath().resolve(this.getCustomPath() + ".launch");
		writeTemplate(cfgFile, "/templates/eclipse_java_template.xml", params);
	}
}
