package io.github.astrarre.amalgamation.gradle.utils.casmerge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

// todo test heuristic on identical classes, mc and mine
public class CASMerger extends ClassVisitor {
	static final FieldVisitor FIELD_VISITOR = new FieldVisitor(Opcodes.ASM9, null) {};
	static final MethodVisitor METHOD_VISITOR = new MethodVisitor(Opcodes.ASM9, null) {};

	final ClassNode server;
	final Handler handler;
	final boolean checkForServerOnly;

	public CASMerger(ClassVisitor visitor, int api, ClassNode server, Handler handler, boolean checkForServerOnly) {
		super(api, visitor);
		this.server = server;
		this.handler = handler;
		this.checkForServerOnly = checkForServerOnly;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, final String[] interfaces) {
		List<String> serverOnly = null;
		List<String> serverInterfaces = this.server.interfaces;
		if(this.checkForServerOnly) {
			for(String iface : serverInterfaces) {
				int index = this.indexOf(interfaces, iface);
				if(index == -1) {
					if(serverOnly == null) {
						serverOnly = new ArrayList<>();
					}
					serverOnly.add(iface);
					this.handler.accept(this.visitAnnotation(this.handler.ifaceDesc(), false), iface, false);
				}
			}
		}

		if(serverOnly != null) {
			int serverOnlyInterfaces = serverOnly.size();
			final String[] ifaces = Arrays.copyOf(interfaces, interfaces.length + serverOnlyInterfaces);
			for(int i = 0; i < serverOnlyInterfaces; i++) {
				ifaces[i + interfaces.length] = serverOnly.get(i);
			}
			super.visit(version, access, name, signature, superName, ifaces);
		} else {
			super.visit(version, access, name, signature, superName, interfaces);
		}

		for(String iface : interfaces) {
			int index = this.indexOf(serverInterfaces, iface);
			if(index == -1) {
				this.handler.accept(this.visitAnnotation(this.handler.ifaceDesc(), false), iface, true);
			}
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		List<InnerClassNode> inners = this.server.innerClasses;
		if(inners != null) {
			for(InnerClassNode inner : inners) {
				if(Objects.equals(inner.name, name)) {
					return;
				}
			}
			// not found in client, can add
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if(this.inServer(this.server.fields, f -> f.name, f -> f.desc, name, descriptor)) {
			return FIELD_VISITOR;
		} else {
			FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
			this.handler.accept(visitor.visitAnnotation(this.handler.normalDesc(), false), true);
			return visitor;
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if(this.inServer(this.server.methods, f -> f.name, f -> f.desc, name, descriptor)) {
			return METHOD_VISITOR;
		} else {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			this.handler.accept(visitor.visitAnnotation(this.handler.normalDesc(), false), true);
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}

	@Override
	public void visitEnd() {
		List<InnerClassNode> inners = this.server.innerClasses;
		if(inners != null) {
			for(InnerClassNode inner : inners) {
				super.visitInnerClass(inner.name, inner.outerName, inner.innerName, inner.access);
			}
		}

		for(MethodNode method : this.server.methods) {
			method.accept(this);
		}

		for(FieldNode field : this.server.fields) {
			field.accept(this);
		}
	}

	int indexOf(final String[] strings, String string) {
		for(int i = 0; i < strings.length; i++) {
			if(Objects.equals(strings[i], string)) {
				return i;
			}
		}
		return -1;
	}

	int indexOf(final List<String> strings, String string) {
		for(int i = 0, len = strings.size(); i < len; i++) {
			if(Objects.equals(strings.get(i), string)) {
				return i;
			}
		}
		return -1;
	}

	private <T> boolean inServer(List<T> list, Function<T, String> nameGetter, Function<T, String> descGetter, String name, String descriptor) {
		if(list != null) {
			for(T node : list) {
				if(Objects.equals(nameGetter.apply(node), name) && Objects.equals(descGetter.apply(node), descriptor)) {
					return true;
				}
			}
			// not found in client, can add
		}
		return false;
	}

	public interface Handler {
		String normalDesc();

		String ifaceDesc();

		void accept(AnnotationVisitor visitor, boolean isClient);

		void accept(AnnotationVisitor visitor, String iface, boolean isClient);
	}

	public static final FabricHandler FABRIC = new FabricHandler("Lnet/fabricmc/api/Environment;", "Lnet/fabricmc/api/EnvironmentInterface;", "Lnet/fabricmc/api/EnvType;");

	public static class FabricHandler implements Handler {
		final String normalDesc, ifaceDesc, enumDesc;

		public FabricHandler(String normalDesc, String ifaceDesc, String enumDesc) {
			this.normalDesc = normalDesc;
			this.ifaceDesc = ifaceDesc;
			this.enumDesc = enumDesc;
		}

		@Override
		public String normalDesc() {
			return this.normalDesc;
		}

		@Override
		public String ifaceDesc() {
			return this.ifaceDesc;
		}

		@Override
		public void accept(AnnotationVisitor visitor, boolean isClient) {
			visitor.visitEnum("value", this.enumDesc, this.forSide(isClient));
		}

		@Override
		public void accept(AnnotationVisitor visitor, String iface, boolean isClient) {
			visitor.visit("itf", Type.getObjectType(iface));
			visitor.visitEnum("value", this.enumDesc, this.forSide(isClient));
		}

		public String forSide(boolean side) {
			return side ? "CLIENT" : "SERVER";
		}
	}

}
