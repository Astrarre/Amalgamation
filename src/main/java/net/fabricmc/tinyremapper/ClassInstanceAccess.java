package net.fabricmc.tinyremapper;

import net.fabricmc.tinyremapper.api.TrClass;

public class ClassInstanceAccess {
	public static InputTag[] getInputTags(TrClass type) {
		return ((ClassInstance)type).getInputTags();
	}
}
