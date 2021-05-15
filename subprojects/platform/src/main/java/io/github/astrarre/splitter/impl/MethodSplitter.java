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
import org.objectweb.asm.tree.MethodNode;

public class MethodSplitter extends Splitter {
	public MethodSplitter(Map<String, ?> properties) {
		super(properties);
	}

	@Override
	public boolean split(ClassNode input, PlatformId forPlatform, ClassNode target) {
		target.methods = new ArrayList<>(input.methods);
		Iterator<MethodNode> iterator = target.methods.iterator();
		while (iterator.hasNext()) {
			MethodNode method = iterator.next();
			if (method.invisibleAnnotations == null) {
				continue;
			}
			if (!SplitterUtil.matches(method.invisibleAnnotations, forPlatform)) {
				iterator.remove();
				continue;
			}

			for (AnnotationNode annotation : method.invisibleAnnotations) {
				if(Classes.DISPLACE_DESC.equals(annotation.desc)) {
					method.name = AsmUtil.get(annotation, "value", method.name);
				}
			}
		}
		return false;
	}
}
