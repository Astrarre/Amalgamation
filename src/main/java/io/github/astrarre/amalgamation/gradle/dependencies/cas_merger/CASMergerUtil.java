package io.github.astrarre.amalgamation.gradle.dependencies.cas_merger;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.astrarre.amalgamation.gradle.dependencies.mojmerge.Key2Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Client Server Merger Utility
 */
public class CASMergerUtil {
	final Map<String, ClassEntry> serverClasses = new ConcurrentHashMap<>();
	final SideAnnotationHandler handler;
	final boolean checkForServerOnly;
	
	public CASMergerUtil(SideAnnotationHandler handler, boolean only) {
		this.handler = handler;
		this.checkForServerOnly = only;
	}
	
	public ServerCollector serverScan() {
		return new ServerCollector();
	}
	
	public ClassAnnotater clientApplier() {
		return new ClassAnnotater();
	}
	
	record ClassEntry(Set<String> interfaces, Key2Set<String, String> methods, Key2Set<String, String> fields, Set<String> innerClasses) {}
	
	public static class ServerCollectingVisitor extends ClassVisitor {
		final ClassEntry current;
		
		public ServerCollectingVisitor(int api, ClassEntry current) {
			super(api);
			this.current = current;
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.current.interfaces.addAll(Arrays.asList(interfaces));
		}
		
		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			this.current.innerClasses.add(name);
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			this.current.fields.put(name, descriptor);
			return null;
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			this.current.methods.put(name, descriptor);
			return null;
		}
	}
	
	public class ClassAnnotater {
		public ByteBuffer apply(String path, ByteBuffer buffer) {
			ClassEntry cls = CASMergerUtil.this.serverClasses.get(path);
			byte[] buf = buffer.array();
			byte[] code;
			ClassReader clientReader = new ClassReader(buf, buffer.arrayOffset(), buffer.capacity());
			ClassWriter writer = new ClassWriter(clientReader, 0);
			if(cls != null) {
				CASMerger merger = new CASMerger(writer, Opcodes.ASM9, cls, CASMergerUtil.this.handler, CASMergerUtil.this.checkForServerOnly);
				clientReader.accept(merger, 0);
			} else {
				clientReader.accept(writer, 0);
				CASMergerUtil.this.handler.accept(writer.visitAnnotation(CASMergerUtil.this.handler.normalDesc(), false), true);
			}
			code = writer.toByteArray();
			return ByteBuffer.wrap(code);
		}
	}
	
	public class ServerCollector {
		public void collect(String path, ByteBuffer buffer) {
			ClassEntry clsEntry = new ClassEntry(new HashSet<>(), new Key2Set<>(4), new Key2Set<>(4), new HashSet<>());
			CASMergerUtil.this.serverClasses.put(path, clsEntry);
			ServerCollectingVisitor visitor = new ServerCollectingVisitor(Opcodes.ASM9, clsEntry);
			ClassReader serverReader = new ClassReader(buffer.array(), buffer.arrayOffset(), buffer.capacity());
			serverReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		}
	}
}
