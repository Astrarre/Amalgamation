package io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.src;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.collect.Iterables;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.coolmineman.trieharder.FindReplaceSourceRemapper;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.impl.util.U;
import net.devtech.zipio.processors.entry.ProcessResult;
import net.devtech.zipio.processors.entry.ZipEntryProcessor;
import org.jetbrains.annotations.NotNull;

public class TrieHarderRemapper implements AmalgRemapper {
	FindReplaceSourceRemapper sourceRemapper;

	@Override
	public void init(List<Mappings.Namespaced> tree) {
		Mappings.Namespaced namespaced = Iterables.getOnlyElement(tree);
		this.sourceRemapper = new FindReplaceSourceRemapper(namespaced.tree(), namespaced.fromI(), namespaced.toI());
	}

	@Override
	public boolean needsClasspath() {
		return false;
	}

	@Override
	public ZipEntryProcessor classpath() {
		throw new UnsupportedOperationException("classpath");
	}

	@Override
	public Remap remap() {
		return new RemapImpl();
	}

	public class RemapImpl implements Remap {
		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			String path = buffer.path();
			if(path.endsWith(".java")) {
				ByteBuffer buf = buffer.read();
				buf.rewind(); // todo add to zip-io
				Reader reader = new StringReader(StandardCharsets.UTF_8.decode(buf).toString());
				var baos = new ByteArrayOutputStream(buf.position()) {
					public ByteBuffer getBytes() { // avoid copying
						return ByteBuffer.wrap(this.buf, 0, this.count);
					}
				};
				try(Writer writer = new OutputStreamWriter(baos)) {
					TrieHarderRemapper.this.sourceRemapper.remap(reader, writer);
				} catch(IOException e) {
					throw U.rethrow(e);
				}
				buffer.writeToOutput(baos.getBytes());
			}
			return ProcessResult.HANDLED;
		}

		@Override
		public void apply(ZipOutput output) {
		}
	}
}
