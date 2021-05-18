/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.configuration.ide;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamation;
import io.github.astrarre.amalgamation.utils.OS;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.ApiStatus;

/**
 * Experimental for now, please make sure to direct any suggests towards the github.
 */
@ApiStatus.Experimental
public final class RunConfigSettings implements Named {
	public static final String KNOT_CLIENT = "net.fabricmc.loader.launch.knot.KnotClient";
	public static final String KNOT_SERVER = "net.fabricmc.loader.launch.knot.KnotServer";

	/**
	 * Arguments for the JVM, such as system properties.
	 */
	private final List<String> vmArgs = new ArrayList<>();

	/**
	 * Arguments for the program's main class.
	 */
	private final List<String> programArgs = new ArrayList<>();

	/**
	 * The environment (or side) to run, usually client or server.
	 */
	private String environment;

	/**
	 * The full name of the run configuration, i.e. 'Minecraft Client'.
	 *
	 * <p>By default this is determined from the base name.
	 */
	private String name;

	/**
	 * The default main class of the run configuration.
	 *
	 * <p>This can be overwritten in {@code fabric_installer.[method].json}. Note that this <em>doesn't</em> take
	 * priority over the main class specified in the Fabric installer configuration.
	 */
	private String defaultMainClass;

	/**
	 * The source set getter, which obtains the source set from the given project.
	 */
	private Function<Project, SourceSet> source;

	/**
	 * The run directory for this configuration, relative to the root project directory.
	 */
	private String runDir;

	/**
	 * The base name of the run configuration, which is the name it is created with, i.e. 'client'
	 */
	private final String baseName;

	/**
	 * When true a run configuration file will be generated for IDE's.
	 *
	 * <p>By default only run configs on the root project will be generated.
	 */
	private boolean ideConfigGenerated;

	private final Project project;
	private final BaseAmalgamation extension;

	public RunConfigSettings(Project project, String baseName) {
		this.baseName = baseName;
		this.project = project;
		this.extension = (BaseAmalgamation) project.getExtensions().getByName("ag");
		this.ideConfigGenerated = this.project == project.getRootProject();

		this.source("main");
		this.runDir("run");
	}

	public Project getProject() {
		return this.project;
	}

	public BaseAmalgamation getExtension() {
		return this.extension;
	}

	@Override
	public String getName() {
		return this.baseName;
	}

	public List<String> getVmArgs() {
		return this.vmArgs;
	}

	public List<String> getProgramArgs() {
		return this.programArgs;
	}

	public String getEnvironment() {
		return this.environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getConfigName() {
		return this.name;
	}

	public void setConfigName(String name) {
		this.name = name;
	}

	public String getDefaultMainClass() {
		return this.defaultMainClass;
	}

	public void setDefaultMainClass(String defaultMainClass) {
		this.defaultMainClass = defaultMainClass;
	}

	public String getRunDir() {
		return this.runDir;
	}

	public void setRunDir(String runDir) {
		this.runDir = runDir;
	}

	public SourceSet getSource(Project proj) {
		return this.source.apply(proj);
	}

	public void setSource(SourceSet source) {
		this.source = proj -> source;
	}

	public void setSource(Function<Project, SourceSet> sourceFn) {
		this.source = sourceFn;
	}

	public void environment(String environment) {
		this.setEnvironment(environment);
	}

	public void name(String name) {
		this.setConfigName(name);
	}

	public void defaultMainClass(String cls) {
		this.setDefaultMainClass(cls);
	}

	public void runDir(String dir) {
		this.setRunDir(dir);
	}

	public void vmArg(String arg) {
		this.vmArgs.add(arg);
	}

	public void vmArgs(String... args) {
		this.vmArgs.addAll(Arrays.asList(args));
	}

	public void vmArgs(Collection<String> args) {
		this.vmArgs.addAll(args);
	}

	public void property(String name, String value) {
		this.vmArg("-D" + name + "=" + value);
	}

	public void property(String name) {
		this.vmArg("-D" + name);
	}

	public void properties(Map<String, String> props) {
		props.forEach(this::property);
	}

	public void programArg(String arg) {
		this.programArgs.add(arg);
	}

	public void programArgs(String... args) {
		this.programArgs.addAll(Arrays.asList(args));
	}

	public void programArgs(Collection<String> args) {
		this.programArgs.addAll(args);
	}

	public void source(SourceSet source) {
		this.setSource(source);
	}

	public void source(String source) {
		this.setSource(proj -> {
			JavaPluginConvention conv = proj.getConvention().getPlugin(JavaPluginConvention.class);
			return conv.getSourceSets().getByName(source);
		});
	}

	public void ideConfigGenerated(boolean ideConfigGenerated) {
		this.ideConfigGenerated = ideConfigGenerated;
	}

	/**
	 * Add the {@code -XstartOnFirstThread} JVM argument when on OSX.
	 */
	public void startFirstThread() {
		if (getOS().equalsIgnoreCase("osx")) {
			this.vmArg("-XstartOnFirstThread");
		}
	}

	public static String getOS() {
		return OS.ACTIVE.launchermetaName;
	}

	/**
	 * Removes the {@code nogui} argument for the server configuration. By default {@code nogui} is specified, this is
	 * a convenient way to remove it if wanted.
	 */
	public void serverWithGui() {
		this.programArgs.removeIf("nogui"::equals);
	}

	/**
	 * Configure run config with the default client options.
	 */
	public void client() {
		this.startFirstThread();
		this.environment("client");
		this.defaultMainClass(KNOT_CLIENT);
	}

	/**
	 * Configure run config with the default server options.
	 */
	public void server() {
		this.programArg("nogui");
		this.environment("server");
		this.defaultMainClass(KNOT_SERVER);
	}

	/**
	 * Copies settings from another run configuration.
	 * @param parent the run config parent
	 */
	public void inherit(RunConfigSettings parent) {
		this.vmArgs.addAll(0, parent.vmArgs);
		this.programArgs.addAll(0, parent.programArgs);

		this.environment = parent.environment;
		this.name = parent.name;
		this.defaultMainClass = parent.defaultMainClass;
		this.source = parent.source;
		this.ideConfigGenerated = parent.ideConfigGenerated;
	}

	public void makeRunDir() {
		File file = new File(this.getProject().getRootDir(), this.runDir);

		if (!file.exists()) {
			file.mkdir();
		}
	}

	public boolean isIdeConfigGenerated() {
		return this.ideConfigGenerated;
	}

	public void setIdeConfigGenerated(boolean ideConfigGenerated) {
		this.ideConfigGenerated = ideConfigGenerated;
	}
}