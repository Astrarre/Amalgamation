package io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.cls;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.remapper.AbstractBinRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import net.devtech.zipio.VirtualZipEntry;
import net.devtech.zipio.ZipOutput;
import net.devtech.zipio.processors.entry.ProcessResult;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.accesswidener.ForwardingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyRemapperInternalAccess;

public class TRemapper extends AbstractBinRemapper {
	public static final Field EXECUTOR;

	static {
		try {
			EXECUTOR = TinyRemapper.class.getDeclaredField("threadPool");
			EXECUTOR.setAccessible(true);
		} catch(NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	TinyRemapper remapper;
	Remapper simpleRemapper;

	@Override
	public void init(List<Mappings.Namespaced> mappings) {
		super.init(mappings);
		IMappingProvider from = Mappings.from(mappings);
		this.remapper = TinyRemapper.newRemapper().withMappings(from).build();
		this.simpleRemapper = new Remapper() {
			@Override
			public String map(String internalName) {
				for(Mappings.Namespaced mapping : mappings) {
					String name = mapping.tree().mapClassName(internalName, mapping.fromI(), mapping.toI());
					if(!name.equals(internalName)) {
						return name;
					}
				}
				return super.map(internalName);
			}

			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				for(Mappings.Namespaced mapping : mappings) {
					MappingTree.ClassMapping cls = mapping.tree().getClass(owner, mapping.fromI());
					if(cls == null) continue;
					var method = cls.getMethod(name, descriptor, mapping.fromI());
					if(method == null) continue;
					String mapped = method.getName(mapping.toI());
					if(!name.equals(mapped)) {
						return mapped;
					}
				}
				return super.mapMethodName(owner, name, descriptor);
			}

			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				for(Mappings.Namespaced mapping : mappings) {
					MappingTree.ClassMapping cls = mapping.tree().getClass(owner, mapping.fromI());
					if(cls == null) continue;
					var field = cls.getField(name, descriptor, mapping.fromI());
					if(field == null) continue;
					String mapped = field.getName(mapping.toI());
					if(!name.equals(mapped)) {
						return mapped;
					}
				}
				return super.mapFieldName(owner, name, descriptor);
			}
		};
		/*try {
			EXECUTOR.set(this.remapper, AmalgIO.SERVICE);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}*/
	}

	@Override
	public Remap remap() {
		return new RemapExt(this.remapper.createInputTag());
	}

	@Override
	public void hash(Hasher hasher) {
		super.hash(hasher);
		hasher.putString("TinyRemapper", StandardCharsets.UTF_8);
	}

	@Override
	protected void readFileToClassPath(String classFile, ByteBuffer data) {
		this.remapper.readFileToClassPath(null, classFile, data.array(), data.arrayOffset(), data.limit());
	}

	@Override
	protected void readFileToInput(RemapImpl remapData, String path, ByteBuffer data) {
		this.remapper.readFileToInput(((RemapExt)remapData).tag, path, data.array(), data.arrayOffset(), data.limit());
	}

	@Override
	protected void write(RemapImpl remapData, ZipOutput output) {
		TinyRemapperInternalAccess.setAllDirty(this.remapper);
		this.remapper.apply((s, b) -> output.write(s + ".class", ByteBuffer.wrap(b)), ((RemapExt)remapData).tag);
		TinyRemapperInternalAccess.unmarkAllAsInput(this.remapper, false);
	}

	@Override
	protected void readNonClassToInput(RemapImpl remapData, String path, ByteBuffer buffer) {
		super.readNonClassToInput(remapData, path, buffer);
	}

	public class RemapExt extends RemapImpl {
		final InputTag tag;
		record Remapped(String file, ByteBuffer remapped) {}
		final List<Remapped> entries = new ArrayList<>();

		RemapExt(InputTag input) {
			super();
			this.tag = input;
		}

		@Override
		public ProcessResult apply(VirtualZipEntry buffer) {
			String path = buffer.path();
			if(path.endsWith(".aw") || path.endsWith(".accesswidener") || path.endsWith(".accessWidener")) {
				ByteBuffer contents = buffer.read();
				AccessWidenerWriter writer = new AccessWidenerWriter();
				AccessWidenerRemapper accessRemapper = new AccessWidenerRemapper(writer, simpleRemapper, null, null);
				ForwardingVisitor visitor = new ForwardingVisitor(accessRemapper) {
					@Override
					public void visitHeader(String namespace) {
						writer.visitHeader("named");
					}
				};
				AccessWidenerReader reader = new AccessWidenerReader(visitor);
				byte[] arr = new byte[contents.remaining()];
				contents.get(arr);
				reader.read(arr);

				this.entries.add(new Remapped(path, ByteBuffer.wrap(writer.write())));
				return ProcessResult.HANDLED;
			} else if(path.contains("META-INF")) {
				return ProcessResult.HANDLED;
			}
			return super.apply(buffer);
		}

		@Override
		public void apply(ZipOutput output) {
			super.apply(output);
			for(Remapped entry : this.entries) {
				output.write(entry.file, entry.remapped);
			}
		}
	}

	@Override
	public void close() {

	}
}
