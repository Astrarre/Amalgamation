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
		Integer index = getIndex(version, name);
		if(index == null) {
			return false;
		}
		String trueName = name.substring(0, index);
		String trueVersion = name.substring(index + 1);
		mutator.set(trueName, trueVersion);
		return false;
	}

	public static Integer getIndex(String version, String name) {
		int i = version.lastIndexOf('_');
		if(i == -1) {
			return null;
		}
		String indexStr = version.substring(i + 1);
		int index;
		try {
			index = Integer.parseInt(indexStr);
		} catch(NumberFormatException e) {
			return null;
		}

		if(name.charAt(index) != '_') {
			return null;
		}
		return index;
	}
}
