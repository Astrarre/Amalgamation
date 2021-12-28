package io.github.astrarre.amalgamation.gradle.dependencies.decomp.fernflower.fabric;

import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;

public final class EmptyFabricJavadocProvider implements IFabricJavadocProvider {
	public static final EmptyFabricJavadocProvider INSTANCE = new EmptyFabricJavadocProvider();
	private EmptyFabricJavadocProvider() {}
	@Override
	public String getClassDoc(StructClass structClass) {
		return null;
	}

	@Override
	public String getFieldDoc(StructClass structClass, StructField structField) {
		return null;
	}

	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		return null;
	}
}
