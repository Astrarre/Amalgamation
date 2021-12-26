package io.github.astrarre.amalgamation.gradle.dependencies.remap.util;

import java.util.List;

import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.mappingio.tree.MappingTree;

public class BasicRemapper extends Remapper {
	private final List<Mappings.Namespaced> mappings;

	public BasicRemapper(List<Mappings.Namespaced> mappings) {this.mappings = mappings;}

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		for(Mappings.Namespaced mapping : mappings) {
			MappingTree.ClassMapping cls = mapping.tree().getClass(owner, mapping.fromI());
			if(cls == null) {
				continue;
			}
			var method = cls.getMethod(name, descriptor, mapping.fromI());
			if(method == null) {
				continue;
			}
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
			if(cls == null) {
				continue;
			}
			var field = cls.getField(name, descriptor, mapping.fromI());
			if(field == null) {
				continue;
			}
			String mapped = field.getName(mapping.toI());
			if(!name.equals(mapped)) {
				return mapped;
			}
		}
		return super.mapFieldName(owner, name, descriptor);
	}

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
}
