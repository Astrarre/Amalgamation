package io.github.astrarre.amalgamation.gradle.merger.api.classes;

import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.amalgamation.gradle.merger.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.merger.api.Platformed;

public class RawPlatformJava extends Platformed<TypeDeclaration<?>> {
	public RawPlatformJava(PlatformId platforms, TypeDeclaration<?> val) {
		super(platforms, val);
	}
}
