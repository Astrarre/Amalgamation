package io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper;

import java.nio.ByteBuffer;
import java.util.List;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;

public abstract class AbstractRemapper implements AmalgRemapper {
	Classpath classpath;
	AmalgRemapper sourceRemapper;

	public void setSourceRemapper(AmalgRemapper sourceRemapper) {
		this.sourceRemapper = sourceRemapper;
	}

	@Override
	public void init(List<Mappings.Namespaced> mappings) {
		this.classpath = new Classpath();
	}

	@Override
	public ZipEntryProcessor classpath() {
		return this.classpath;
	}

	@Override
	public Remap remap() {
		return new RemapImpl();
	}

	protected abstract void readFileToClassPath(String classFile, ByteBuffer data);

	protected abstract void readFileToInput(RemapImpl remapData, String path, ByteBuffer buffer);

	protected abstract void write(RemapImpl remapData, ZipOutput output);

	public class Classpath implements ZipEntryProcessor {
		final ZipEntryProcessor srcProc;

		{
			if(AbstractRemapper.this.sourceRemapper != null && AbstractRemapper.this.sourceRemapper.needsClasspath()) {
				this.srcProc = AbstractRemapper.this.sourceRemapper.classpath();
			} else {
				this.srcProc = null;
			}
		}

		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			if(buffer.path().endsWith(".class")) {
				ByteBuffer read = buffer.read();
				AbstractRemapper.this.readFileToClassPath(buffer.path(), read);
			}
			if(this.srcProc != null) {
				return this.srcProc.apply(buffer);
			} else {
				return ProcessResult.HANDLED; // prevent classpath from being written
			}
		}
	}

	public class RemapImpl implements Remap {
		final Remap srcProc = AbstractRemapper.this.sourceRemapper != null ? AbstractRemapper.this.sourceRemapper.remap() : null;


		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			String path = buffer.path();
			if(path.endsWith(".class")) {
				ByteBuffer read = buffer.read();
				AbstractRemapper.this.readFileToInput(this, path, read);
			} else if(this.srcProc == null) {
				buffer.copyToOutput();
			} else {
				return this.srcProc.apply(buffer);
			}
			return ProcessResult.HANDLED;
		}

		@Override
		public void apply(ZipOutput output) {
			AbstractRemapper.this.write(this, output);
			if(this.srcProc != null) {
				this.srcProc.apply(output);
			}
		}
	}
}
