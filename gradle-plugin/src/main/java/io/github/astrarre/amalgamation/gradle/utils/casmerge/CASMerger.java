package io.github.astrarre.amalgamation.gradle.utils.casmerge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

// todo test heuristic on identical classes, mc and mine
public class CASMerger extends ClassVisitor {
	static final FieldVisitor FIELD_VISITOR = new FieldVisitor(Opcodes.ASM9, null) {};
	static final MethodVisitor METHOD_VISITOR = new MethodVisitor(Opcodes.ASM9, null) {};

	final CASMergerUtil.ClassEntry server;
	final Handler handler;
	final boolean checkForServerOnly;

	public CASMerger(ClassVisitor visitor, int api, CASMergerUtil.ClassEntry server, Handler handler, boolean checkForServerOnly) {
		super(api, visitor);
		this.server = server;
		this.handler = handler;
		this.checkForServerOnly = checkForServerOnly;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, final String[] interfaces) {
		List<String> serverOnly = null;
		Set<String> serverInterfaces = this.server.interfaces();
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
			if(!serverInterfaces.contains(iface)) {
				this.handler.accept(this.visitAnnotation(this.handler.ifaceDesc(), false), iface, true);
			}
		}
	}


	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if(this.server.fields().contains(name, descriptor)) {
			return super.visitField(access, name, descriptor, signature, value);
		} else {
			FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
			this.handler.accept(visitor.visitAnnotation(this.handler.normalDesc(), false), true);
			return visitor;
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if(this.server.methods().contains(name, descriptor)) {
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		} else {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			this.handler.accept(visitor.visitAnnotation(this.handler.normalDesc(), false), true);
			return visitor;
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

	public static class Config {
		final DependencyHandler project;
		public String version;
		public Handler handler = FABRIC;
		public int clsReaderFlags = 0;
		public boolean checkForServerOnly = false;
		public Dependency client;
		public Dependency server;

		public Config(Project project) {this.project = project.getDependencies();}

		public Config client(Object client, Closure<ModuleDependency> config) {
			this.client = this.project.create(client, config);
			return this;
		}

		public Config server(Object server, Closure<ModuleDependency> config) {
			this.server = this.project.create(server, config);
			return this;
		}

		public Config client(Object client) {
			this.client = this.project.create(client);
			return this;
		}

		public Config server(Object server) {
			this.server = this.project.create(server);
			return this;
		}
	}
}
