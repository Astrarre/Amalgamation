package io.github.astrarre.amalgamation.gradle.utils.mojmerge;

import io.github.astrarre.amalgamation.gradle.utils.casmerge.CASMerger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;

public class MojMerger extends ClassVisitor {
	final CASMerger.Handler handler;
	final MappingTree serverMappings;

	MappingTreeView.ClassMappingView owner;

	public MojMerger(int api, ClassVisitor classVisitor, CASMerger.Handler handler, MappingTree reader) {
		super(api, classVisitor);
		this.handler = handler;
		this.serverMappings = reader;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		MappingTreeView.ClassMappingView view = this.serverMappings.getClass(name, 0);
		if(view == null) {
			this.handler.accept(this.visitAnnotation(this.handler.normalDesc(), false), true);
		} else {
			this.owner = view;
		}

		for(String iface : interfaces) {
			if(this.serverMappings.getClass(iface, 0) == null) {
				// interface not present on server
				this.handler.accept(this.visitAnnotation(this.handler.ifaceDesc(), false), iface, true);
			}
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
		if(this.owner != null && this.owner.getMethod(name, descriptor, 0) == null) {
			this.handler.accept(visitor.visitAnnotation(this.handler.normalDesc(), false), true);
		}
		return visitor;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
		if(this.owner != null && this.owner.getField(name, descriptor, 0) == null) {
			this.handler.accept(visitor.visitAnnotation(this.handler.normalDesc(), false), true);
		}
		return visitor;
	}
}
