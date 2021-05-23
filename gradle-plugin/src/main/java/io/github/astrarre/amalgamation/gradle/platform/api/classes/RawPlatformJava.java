package io.github.astrarre.amalgamation.gradle.platform.api.classes;

import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.amalgamation.gradle.platform.api.PlatformId;
import io.github.astrarre.amalgamation.gradle.platform.api.Platformed;

public class RawPlatformJava extends Platformed<TypeDeclaration<?>> {
	public RawPlatformJava(PlatformId platforms, TypeDeclaration<?> val) {
		super(platforms, val);
	}
}
