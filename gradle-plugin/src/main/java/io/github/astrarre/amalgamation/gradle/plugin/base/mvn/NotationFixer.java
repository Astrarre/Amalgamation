package io.github.astrarre.amalgamation.gradle.plugin.base.mvn;

import org.jetbrains.annotations.Nullable;

public final class NotationFixer implements MvnMetaReader.DependencyVisitor {
	public static final NotationFixer INSTANCE = new NotationFixer();

	private NotationFixer() {}

	@Override
	public boolean apply(@Nullable String group, String name, @Nullable String version, MvnMetaReader.Mutator mutator) {
		if(version == null) {
			return false;
		}
		Integer index = ModuleJsonFixer.getIndex(version, name);
		if(index == null) {
			return false;
		}
		String trueName = name.substring(0, index);
		String trueVersion = name.substring(index + 1);
		mutator.set(trueName, trueVersion);
		return false;
	}
}
