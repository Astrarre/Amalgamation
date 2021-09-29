package io.github.astrarre.amalgamation.gradle.files;

import java.io.IOException;
import java.nio.file.Path;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.util.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMergerUtil;
import net.devtech.zipio.processes.ZipProcess;
import net.devtech.zipio.processes.ZipProcessBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

/**
 * Server Client Merged
 */
public class CASMergedFile extends ZipProcessCachedFile {
	public final Project project;
	public final String version;
	public final CASMerger.Handler handler;
	public final int classReaderSettings;
	public final boolean checkForServerOnly;
	public final Dependency server;
	public final Dependency client;

	public CASMergedFile(Path output,
			Project project,
			String version,
			CASMerger.Handler handler,
			int settings,
			boolean only,
			Dependency server,
			Dependency client) {
		super(output, project);
		this.project = project;
		this.version = version;
		this.handler = handler;
		this.classReaderSettings = settings;
		this.checkForServerOnly = only;
		this.server = server;
		this.client = client;
	}

	@Override
	public void init(ZipProcessBuilder process, Path outputFile) throws IOException {
		CASMergerUtil merger = new CASMergerUtil(this.handler, this.checkForServerOnly);
		// todo use intermediary to detect lib classes to avoid annotating them
		ZipProcessBuilder scan = ZipProcess.builder();
		ZipProcessable.add(this.project, scan, this.server, p -> null);
		scan.setEntryProcessor(merger.serverScan());
		scan.execute();
		process.setEntryProcessor(merger.clientApplier());
		ZipProcessable.add(this.project, process, this.client, p -> null);
	}

	@Override
	public String toString() {
		return "Merge " + this.version;
	}

	@Override
	public void hashInputs(Hasher hasher) {
		AmalgIO.hash(this.project, hasher, this.server);
		AmalgIO.hash(this.project, hasher, this.client);
	}

	@Override
	protected void write(Path output) {

	}
}
