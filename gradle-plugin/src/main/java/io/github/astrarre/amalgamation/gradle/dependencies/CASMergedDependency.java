package io.github.astrarre.amalgamation.gradle.dependencies;


import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMergerUtil;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public class CASMergedDependency extends ZipProcessDependency {
	public boolean global = true, checkForServerOnly = false;
	public CASMerger.Handler handler;
	public Object client, server;

	public CASMergedDependency(Project project, String version) {
		super(project, "io.github.astrarre.amalgmation", "cas-merge", version);
	}

	@Override
	protected void add(ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			CASMergerUtil merger = new CASMergerUtil(this.handler, this.checkForServerOnly);

			// scan server
			ZipProcessBuilder scan = ZipProcess.builder();
			ZipProcessable.add(this.project, scan, this.of(this.server), p -> OutputTag.INPUT);
			scan.setEntryProcessor(merger.serverScan());
			scan.execute();

			// iterate client
			process.setEntryProcessor(merger.clientApplier());
			ZipProcessable.add(this.project, process, this.of(this.client), p -> tag(resolvedPath));
		} else {
			process.addProcessed(resolvedPath);
		}
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		Objects.requireNonNull(this.client, "client dependency was not set!");
		Objects.requireNonNull(this.server, "server dependency was not set!");
		this.client = this.hashDep(hasher, this.client);
		this.server = this.hashDep(hasher, this.server);
	}

	@Override
	protected Path evaluatePath(byte[] hash) {
		String dir = AmalgIO.b64(hash);
		Dependency client = this.of(this.client), server = this.of(this.server);
		String name = client.getName() + "-" + server.getName() + "@" + client.getVersion() + "_" + server.getVersion();
		return AmalgIO.cache(this.project, this.global).resolve(this.version).resolve("merge").resolve(name + "-" + dir + ".jar");
	}
}
