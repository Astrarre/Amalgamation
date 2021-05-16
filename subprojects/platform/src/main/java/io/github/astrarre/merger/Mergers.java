package io.github.astrarre.merger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.astrarre.merger.impl.AccessMerger;
import io.github.astrarre.merger.impl.ClassMerger;
import io.github.astrarre.merger.impl.HeaderMerger;
import io.github.astrarre.merger.impl.InnerClassAttributeMerger;
import io.github.astrarre.merger.impl.InterfaceMerger;
import io.github.astrarre.merger.impl.SignatureMerger;
import io.github.astrarre.merger.impl.SuperclassMerger;
import io.github.astrarre.merger.impl.field.FieldMerger;
import io.github.astrarre.merger.impl.method.MethodMerger;

public class Mergers {
	public static List<Merger> defaults(Map<String, ?> config) {
		List<Merger> mergers = new ArrayList<>(); // order matters sometimes
		mergers.add(new AccessMerger(config));
		mergers.add(new ClassMerger(config));
		mergers.add(new HeaderMerger(config));
		mergers.add(new InnerClassAttributeMerger(config));
		mergers.add(new InterfaceMerger(config));
		mergers.add(new SuperclassMerger(config));
		mergers.add(new SignatureMerger(config));
		mergers.add(new MethodMerger(config));
		mergers.add(new FieldMerger(config));
		return mergers;
	}
}
