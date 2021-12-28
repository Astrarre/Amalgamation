package io.github.astrarre.amalgamation.gradle.dependencies.decomp.fernflower.fabric;

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;

public final class CombinedFabricJavadocProvider implements IFabricJavadocProvider {
	public static IFabricJavadocProvider combine(List<IFabricJavadocProvider> list) {
		if(list.isEmpty()) {
			return EmptyFabricJavadocProvider.INSTANCE;
		} else if(list.size() == 1) {
			return list.get(0);
		} else {
			return new CombinedFabricJavadocProvider(list);
		}
	}

	final List<IFabricJavadocProvider> providers;

	private CombinedFabricJavadocProvider(List<IFabricJavadocProvider> providers) {
		this.providers = providers;
	}

	@Override
	public String getClassDoc(StructClass structClass) {
		return this.getDoc(p -> p.getClassDoc(structClass));
	}

	@Override
	public String getFieldDoc(StructClass structClass, StructField structField) {
		return this.getDoc(p -> p.getFieldDoc(structClass, structField));
	}

	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		return this.getDoc(p -> p.getMethodDoc(structClass, structMethod));
	}

	@Nullable
	private String getDoc(Function<IFabricJavadocProvider, String> documentation) {
		String only = null;
		StringBuilder combined = null;
		for(IFabricJavadocProvider provider : this.providers) {
			String doc = documentation.apply(provider);
			if(doc != null) {
				if(combined == null) {
					if(only == null) {
						only = doc;
						continue;
					} else {
						combined = new StringBuilder(only.length() + doc.length());
						combined.append(only);
						only = null;
					}
				}
				combined.append('\n');
				combined.append(doc);
			}
		}

		if(only != null) {
			return only;
		} else if(combined != null) {
			return combined.toString();
		} else {
			return null;
		}
	}
}
