package io.github.astrarre.api.classes;

import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.astrarre.api.PlatformId;
import io.github.astrarre.api.Platformed;

public class RawPlatformJava extends Platformed<TypeDeclaration<?>> {
	public RawPlatformJava(PlatformId platforms, TypeDeclaration<?> val) {
		super(platforms, val);
	}
}
