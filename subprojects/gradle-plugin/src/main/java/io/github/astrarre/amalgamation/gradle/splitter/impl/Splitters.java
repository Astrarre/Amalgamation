package io.github.astrarre.amalgamation.gradle.splitter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.amalgamation.gradle.splitter.Splitter;

public class Splitters {
	public static List<Splitter> defaults(Map<String, ?> properties) {
		List<Splitter> splitters = new ArrayList<>();
		splitters.add(new AccessSplitter(properties));
		splitters.add(new ClassSplitter(properties));
		splitters.add(new HeaderSplitter(properties));
		splitters.add(new InnerClassAttributeSplitter(properties));
		splitters.add(new InterfaceSplitter(properties));
		splitters.add(new SignatureSplitter(properties));
		splitters.add(new SuperclassSplitter(properties));
		splitters.add(new MethodSplitter(properties));
		splitters.add(new FieldSplitter(properties));
		return splitters;
	}
}
