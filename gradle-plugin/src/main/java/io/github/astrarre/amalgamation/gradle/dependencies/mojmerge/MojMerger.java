package io.github.astrarre.amalgamation.gradle.dependencies.mojmerge;

import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import net.fabricmc.mappingio.tree.MappingTreeView;

public class MojMerger extends ClassVisitor {
	final SideAnnotationHandler handler;
	final Mappings.Namespaced clientMap;
	final Mappings.Namespaced serverMap;

	MappingTreeView.ClassMappingView owner;

	public MojMerger(int api, ClassVisitor classVisitor, SideAnnotationHandler handler, Mappings.Namespaced clientMappings, Mappings.Namespaced reader) {
		super(api, classVisitor);
		this.handler = handler;
		this.clientMap = clientMappings;
		this.serverMap = reader;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		MappingTreeView.ClassMappingView view = this.serverMap.tree().getClass(name, this.serverMap.fromI());
		if(view == null) {
			this.handler.accept(this.visitAnnotation(this.handler.normalDesc(), false), true);
		} else {
			this.owner = view;
		}

		for(String iface : interfaces) {
			boolean isMissing = !iface.startsWith("java"); // if non sdk class

			if(isMissing) { // and not in server mappings
				isMissing = this.serverMap.tree().getClass(iface, this.serverMap.fromI()) == null;
			}

			if(isMissing) { // and not in client mappings
				isMissing = this.clientMap.tree().getClass(iface, this.clientMap.fromI()) != null;
			}

			if(isMissing) {
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
