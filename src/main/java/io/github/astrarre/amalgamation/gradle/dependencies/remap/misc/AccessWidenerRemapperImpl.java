package io.github.astrarre.amalgamation.gradle.dependencies.remap.misc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.accesswidener.AccessWidenerFormatException;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.accesswidener.ForwardingVisitor;

public class AccessWidenerRemapperImpl implements AmalgamationRemapper {
	public static final Set<CharSequence> DEFAULT = Set.of("accessWidener", "aw", "accesswidener", "access", "widener");
	public static final Hash.Strategy<CharSequence> CHAR_SEQUENCE_STRATEGY = new Hash.Strategy<>() {
		@Override
		public int hashCode(CharSequence o) {
			if(o == null) {
				return 0;
			}
			int result = 1;
			for(int i = 0; i < o.length(); i++) {
				char element = o.charAt(i);
				result = 31 * result + element;
			}
			return result;
		}

		@Override
		public boolean equals(CharSequence a, CharSequence b) {
			if(a == b) {
				return true;
			}
			if(a == null || b == null) {
				return false;
			}
			if(a instanceof String s) {
				return s.contentEquals(b);
			} else if(b instanceof String s) {
				return s.contentEquals(a);
			} else if(a.length() == b.length()) {
				for(int i = 0; i < a.length(); i++) {
					if(a.charAt(i) != b.charAt(i)) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}
	};

	public final Set<CharSequence> validExtensions = new ObjectOpenCustomHashSet<>(DEFAULT, CHAR_SEQUENCE_STRATEGY);
	public final Set<String> explicitNames = new HashSet<>();
	Remapper simpleRemapper;
	String namespace;

	public void add(CharSequence validPath) {
		this.validExtensions.add(validPath);
	}

	/**
	 * The access widener remapper doesn't check the fabric.mod.json because I'm lazy so u can add it manually here
	 */
	public void addPath(String sequence) {
		this.explicitNames.add(sequence);
	}

	@Override
	public void acceptMappings(List<Mappings.Namespaced> mappings, Remapper remapper) {
		this.namespace = mappings.get(0).to();
		this.simpleRemapper = remapper;
	}
	
	@Override
	public void acceptRemaps(List<Pair<Artifact, Artifact>> fromTos) throws Exception {
		for(Pair<Artifact, Artifact> to : fromTos) {
			VirtualSource source = to.left().file.openAsSource();
			VirtualSink sink = AmalgIO.DISK_OUT.subsink(to.right().file);
			source.depthStream().filter(VirtualFile.class::isInstance).filter(p -> p.relativePath().endsWith(".java")).forEach(v -> {
				String path = v.relativePath();
				if(this.explicitNames.contains(path) || this.validExtensions.contains(path.subSequence(path.lastIndexOf('.')+1, path.length()))) {
					AccessWidenerWriter writer = new AccessWidenerWriter();
					try {
						ByteBuffer contents = ((VirtualFile) v).getContents();
						AccessWidenerRemapper accessRemapper = new AccessWidenerRemapper(writer, simpleRemapper, null, null);
						ForwardingVisitor visitor = new ForwardingVisitor(accessRemapper) {
							@Override
							public void visitHeader(String namespace) {
								writer.visitHeader(AccessWidenerRemapperImpl.this.namespace);
							}
						};
						AccessWidenerReader reader = new AccessWidenerReader(visitor);
						byte[] arr = new byte[contents.remaining()];
						contents.get(arr);
						reader.read(arr);
					} catch(AccessWidenerFormatException t) {
						if(!path.endsWith(".txt")) {
							System.err.println("Error in remapping " + path);
							t.printStackTrace();
						}
					}
					sink.write(sink.outputFile(path), ByteBuffer.wrap(writer.write()));
				}
			});
		}
	}
	
	@Override
	public void hash(Hasher hasher) {
		hasher.putString("AccessWidenerRemapper", StandardCharsets.UTF_8);
	}

}
