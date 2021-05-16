package io.github.astrarre.amalgamation.mappings_flattener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.astrarre.amalgamation.utils.CachedFile;
import io.github.astrarre.amalgamation.utils.LauncherMeta;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.Descriptored;
import net.fabricmc.mapping.tree.LocalVariableDef;
import net.fabricmc.mapping.tree.ParameterDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;


public class Flattener extends ClassVisitor {
	protected final TinyEmitter emitter;
	protected final String rootNamespace;
	protected final TinyTree tree;
	protected final Map<String, InheritanceData> cachedInheritanceData = new HashMap<>();
	protected final Map<String, List<Consumer<InheritanceData>>> superClassListeners = new HashMap<>();
	InheritanceData currentData;

	public Flattener(TinyEmitter emitter, TinyTree tree) {
		super(Opcodes.ASM9);
		this.emitter = emitter;
		this.tree = tree;
		this.rootNamespace = emitter.namespaces.get(0);
	}

	public static void main(String[] args) throws Exception {
		//TinyTree tree = TinyMappingFactory.load(new BufferedReader(new FileReader(args[0])));
		Logger logger = LoggerFactory.getLogger("mappings-flattener");
		Path cache = Paths.get("cache");
		LauncherMeta meta = new LauncherMeta(cache, logger);
		LauncherMeta.Version version = meta.getVersion(args[2]);
		CachedFile file = CachedFile.forUrl(version.getClientJar(), cache.resolve(version.version + "-client.jar"), logger);
		try (TinyEmitter emitter = new TinyEmitter(new BufferedWriter(new FileWriter(args[1])))) {
			TinyTree tree;
			try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
				tree = TinyMappingFactory.load(reader);
			}
			emitter.start(tree.getMetadata());

			Flattener flattener = new Flattener(emitter, tree);
			try (ZipInputStream jar = new ZipInputStream(Files.newInputStream(file.getPath()))) {
				ZipEntry entry;
				while ((entry = jar.getNextEntry()) != null) {
					if (entry.getName().endsWith(".class")) {
						ClassReader reader = new ClassReader(jar);
						reader.accept(flattener, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
					}
					jar.closeEntry();
				}
			}
		}
	}

	public void listenForParents(String className, Consumer<InheritanceData> listener) {
		if (className.startsWith("java")) {
			return; // minor opt
		}

		InheritanceData cached = this.cachedInheritanceData.get(className);
		if (cached == null) {
			this.superClassListeners.computeIfAbsent(className, s -> new ArrayList<>()).add(listener);
		} else {
			listener.accept(cached);
		}
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		InheritanceData data = new InheritanceData(name, superName, interfaces);
		this.currentData = data;
		this.putData(data);

		super.visit(version, access, name, signature, superName, interfaces);
		ClassDef def = this.tree.getDefaultNamespaceClassMap().get(name);
		this.emitter.pushClass(def);
		this.emitter.pushComment(def.getComment());
	}

	public void putData(InheritanceData data) {
		this.cachedInheritanceData.put(data.name, data);
		List<Consumer<InheritanceData>> listeners = this.superClassListeners.remove(data.name);
		if (listeners != null) {
			for (Consumer<InheritanceData> listener : listeners) {
				listener.accept(data);
			}
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		this.find(ClassDef::getFields, this.currentData, name, descriptor, (root, def) -> {
			this.emitter.pushField(def);
			this.emitter.pushComment(def.getComment());
		});
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		this.find(ClassDef::getMethods, this.currentData, name, descriptor, (root, def) -> {
			this.emitter.pushMethod(def);
			this.emitter.pushComment(def.getComment());
			for (ParameterDef parameter : def.getParameters()) {
				this.emitter.pushParameter(parameter);
				this.emitter.pushComment(parameter.getComment());
			}
			if(root) {
				for (LocalVariableDef variable : def.getLocalVariables()) {
					this.emitter.pushLocalVariable(variable);
				}
			}
		});
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	public <T extends Descriptored> void find(Function<ClassDef, Iterable<T>> getter,
			InheritanceData data,
			String name,
			String desc,
			BiConsumer<Boolean, T> onFind) {
		Map<String, ClassDef> map = this.tree.getDefaultNamespaceClassMap();
		if (this.find(getter.apply(map.get(data.name)), name, desc, onFind, true)) {
			return;
		}
		if (map.containsKey(data.superName) && this.find(getter.apply(map.get(data.superName)), name, desc, onFind, false)) {
			return;
		}
		for (String iface : data.interfaces) {
			if (map.containsKey(iface) && this.find(getter.apply(map.get(iface)), name, desc, onFind, false)) {
				return;
			}
		}

		boolean[] found = {false};
		if(map.containsKey(data.superName)) {
			this.listenForParents(data.superName, inheritanceData -> this.find(getter, inheritanceData, name, desc, (v, t) -> {
				onFind.accept(false, t);
				found[0] = true;
			}));
		}
		if (!found[0]) {
			for (String iface : data.interfaces) {
				if(map.containsKey(iface)) {
					this.listenForParents(iface, inheritanceData -> this.find(getter, inheritanceData, name, desc, (v, t) -> {
						onFind.accept(false, t);
						found[0] = true;
					}));
				}
			}
		}
	}

	protected <T extends Descriptored> boolean find(Iterable<T> iterable, String name, String desc, BiConsumer<Boolean, T> onFind, boolean val) {
		for (T t : iterable) {
			if (name.equals(t.getName(this.rootNamespace)) && desc.equals(t.getDescriptor(this.rootNamespace))) {
				onFind.accept(val, t);
				return true;
			}
		}
		return false;
	}

	public static final class InheritanceData {
		public final String name;
		public final String superName;
		public final String[] interfaces;

		public InheritanceData(String name, String superName, String[] interfaces) {
			this.name = name;
			this.superName = superName;
			this.interfaces = interfaces;
		}
	}
}
