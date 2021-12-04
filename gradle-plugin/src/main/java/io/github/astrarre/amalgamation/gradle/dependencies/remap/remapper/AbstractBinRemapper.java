package io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;

public abstract class AbstractBinRemapper implements AmalgRemapper {
	Classpath classpath;
	AmalgRemapper sourceRemapper;

	public void setSourceRemapper(AmalgRemapper sourceRemapper) {
		this.sourceRemapper = sourceRemapper;
	}

	@Override
	public void init(List<Mappings.Namespaced> mappings) {
		this.classpath = new Classpath();
		if(this.sourceRemapper != null) {
			this.sourceRemapper.init(mappings);
		}
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

	protected void readNonClassToInput(RemapImpl remapData, String path, ByteBuffer buffer) {}

	@Override
	public final boolean needsClasspath() {
		return binNeedsClasspath() || (sourceRemapper != null && sourceRemapper.needsClasspath());
	}

	protected boolean binNeedsClasspath() {
		return true;
	}

	@Override
	public void hash(Hasher hasher) {
		if(this.sourceRemapper != null) {
			this.sourceRemapper.hash(hasher);
		}
	}

	public class Classpath implements ZipEntryProcessor {
		final ZipEntryProcessor srcProc;

		{
			if(AbstractBinRemapper.this.sourceRemapper != null && AbstractBinRemapper.this.sourceRemapper.needsClasspath()) {
				this.srcProc = AbstractBinRemapper.this.sourceRemapper.classpath();
			} else {
				this.srcProc = null;
			}
		}

		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			if(buffer.path().endsWith(".class") && !binNeedsClasspath()) {
				ByteBuffer read = buffer.read();
				AbstractBinRemapper.this.readFileToClassPath(buffer.path(), read);
			}
			if(this.srcProc != null) {
				return this.srcProc.apply(buffer);
			} else {
				return ProcessResult.HANDLED;
			}
		}
	}

	public class RemapImpl implements Remap {
		final Remap srcProc = AbstractBinRemapper.this.sourceRemapper != null ? AbstractBinRemapper.this.sourceRemapper.remap() : null;

		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			String path = buffer.path();
			if(path.endsWith(".class")) {
				ByteBuffer read = buffer.read();
				AbstractBinRemapper.this.readFileToInput(this, path, read);
			} else if(this.srcProc != null) {
				this.srcProc.apply(buffer);
			} else {
				buffer.copyToOutput();
			}
			return ProcessResult.HANDLED;
		}

		@Override
		public void apply(ZipOutput output) {
			AbstractBinRemapper.this.write(this, output);
			if(this.srcProc != null) {
				this.srcProc.apply(output);
			}
		}
	}
}
