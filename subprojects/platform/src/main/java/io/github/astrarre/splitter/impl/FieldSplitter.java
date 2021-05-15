package io.github.astrarre.splitter.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import io.github.astrarre.Classes;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.merger.util.AsmUtil;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.util.SplitterUtil;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class FieldSplitter extends Splitter {
	public FieldSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		target.fields = new ArrayList<>(input.fields);
		Iterator<FieldNode> iterator = target.fields.iterator();
		while (iterator.hasNext()) {
			FieldNode field = iterator.next();
			if (field.invisibleAnnotations == null) {
				continue;
			}
			if (!SplitterUtil.matches(field.invisibleAnnotations, forPlatform)) {
				iterator.remove();
				continue;
			}

			field.invisibleAnnotations = SplitterUtil.stripAnnotations(field.invisibleAnnotations, forPlatform);

			for (AnnotationNode annotation : field.invisibleAnnotations) {
				if(Classes.DISPLACE_DESC.equals(annotation.desc)) {
					field.name = AsmUtil.get(annotation, "value", field.name); // todo technically this should be copied
				}
			}
		}
		return false;
	}
}
