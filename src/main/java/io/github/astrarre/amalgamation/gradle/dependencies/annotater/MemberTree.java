package io.github.astrarre.amalgamation.gradle.dependencies.annotater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.github.astrarre.amalgamation.gradle.dependencies.mojmerge.Key2Set;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import org.objectweb.asm.Type;

public class MemberTree {
	record ClassInstance(boolean included, Key2Set<String, String> members, int fields, int methods) {}

	// todo consider compacting arrays
	public static Map<String, ClassInstance> read(DataInput stream) throws IOException {
		Map<String, ClassInstance> tree = new HashMap<>();

		// read constant pool
		int constantCount = stream.readUnsignedShort();
		String[] constants = new String[constantCount];
		for(int i = 0; i < constantCount; i++) {
			constants[i] = stream.readUTF();
		}

		// read classes who's members are annotated, but not the class itself
		int excludedCount = stream.readUnsignedShort();
		for(int i = 0; i < excludedCount; i++) {
			int classIndex = stream.readUnsignedShort();
			String className = Type.getType(constants[classIndex]).getInternalName(); // we use desc just for compactness
			int fields = stream.readUnsignedShort(), methods = stream.readUnsignedShort();
			ClassInstance type = new ClassInstance(false, new Key2Set<>(fields + methods), fields, methods);

			// read fields
			for(int __ = 0; __ < fields; __++) {
				int nameIndex = stream.readUnsignedShort();
				String name = constants[nameIndex];
				int descIndex = stream.readUnsignedShort();
				String desc = constants[descIndex];
				type.members.put(name, desc);
			}

			// read methods
			for(int __ = 0; __ < methods; __++) {
				int nameIndex = stream.readUnsignedShort();
				String name = constants[nameIndex];

				// read method descriptor
				int args = stream.readUnsignedShort();
				StringBuilder builder = new StringBuilder();
				builder.append('(');
				for(int _$ = 0; _$ < args; _$++) {
					int argId = stream.readUnsignedShort();
					String argType = constants[argId];
					builder.append(argType);
				}
				builder.append(')');
				int returnType = stream.readUnsignedShort();
				String retType = constants[returnType];
				builder.append(retType);

				type.members.put(name, builder.toString());
			}

			tree.put(className, type);
		}

		int included = stream.readUnsignedShort();
		for(int __ = 0; __ < included; __++) {
			int classIndex = stream.readUnsignedShort();
			String className = constants[classIndex];
			ClassInstance type = new ClassInstance(true, null, 0, 0);
			tree.put(className, type);
		}

		return tree;
	}

	public static void write(DataOutput output, Map<String, ClassInstance> map) throws IOException {
		Object2IntLinkedOpenHashMap<String> indexedConstantPool = new Object2IntLinkedOpenHashMap<>();
		var ref = new Object() {int count;};
		Consumer<String> cache = string -> indexedConstantPool.computeIfAbsent(string, __ -> ref.count++);
		buildConstantPool(map, cache);
		// write constant pool
		output.writeShort(indexedConstantPool.size());
		for(String key : indexedConstantPool.keySet()) {
			output.writeUTF(key);
		}

		int includedCount = (int) map.values().stream().filter(ClassInstance::included).count();
		int excludedCount = map.size() - includedCount;

		output.writeInt(excludedCount);
		for(var entry : map.entrySet()) {
			ClassInstance value = entry.getValue();
			if(!value.included()) {
				int id = indexedConstantPool.getInt("L" + entry.getKey() + ";");
				output.writeInt(id);

				
			}
		}

		output.writeInt(includedCount);
		for(var entry : map.entrySet()) {
			if(entry.getValue().included()) {
				int id = indexedConstantPool.getInt("L" + entry.getKey() + ";");
				output.writeInt(id);
			}
		}
	}

	private static void buildConstantPool(Map<String, ClassInstance> map, Consumer<String> cache) {
		map.forEach((internalName, type) -> {
			cache.accept('L' + internalName + ';');
			if(type.members != null) {
				Object[] key1 = type.members.key1, key2 = type.members.key2;
				for(int i = 0; i < key1.length; i++) {
					String name = (String) key1[i];
					cache.accept(name);
					String desc = (String) key2[i];
					if(desc.charAt(0) == '(') {
						Type methodDesc = Type.getMethodType(desc);
						for(Type arg : methodDesc.getArgumentTypes()) {
							cache.accept(arg.getDescriptor());
						}
						cache.accept(methodDesc.getReturnType().getDescriptor());
					} else {
						cache.accept(desc);
					}
				}
			}
		});
	}

}
