package io.github.astrarre.amalgamation.gradle.dependencies.cas_merger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;

public class CASMergedDependency extends ZipProcessDependency {
	public final String version;
	public boolean global = true, checkForServerOnly = false;
	public SideAnnotationHandler handler;
	public Object client, server;

	public CASMergedDependency(Project project, String version) {
		super(project);
		this.version = version;
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			CASMergerUtil merger = new CASMergerUtil(this.handler, this.checkForServerOnly);

			// scan server
			ZipProcessBuilder scan = ZipProcess.builder();
			this.apply(scan, this.of(this.server), p -> OutputTag.INPUT);
			scan.setEntryProcessor(merger.serverScan());
			scan.execute();

			// iterate client
			process.setEntryProcessor(merger.clientApplier());
			resolver.apply(this.of(this.client), p -> p.derive(resolvedPath, this.getCurrentHash()));
		} else {
			process.addProcessed(resolvedPath);
		}
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		Objects.requireNonNull(this.client, "client dependency was not set!");
		Objects.requireNonNull(this.server, "server dependency was not set!");
		this.hashDep(hasher, this.client);
		this.hashDep(hasher, this.server);
	}

	@Override
	protected Path evaluatePath(byte[] hash) {
		String dir = AmalgIO.b64(hash);
		return AmalgIO.cache(this.project, this.global).resolve(this.version).resolve(this.version + "-merged-" + dir + ".jar");
	}
}
