package net.devtech.testbytecodemerge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.element.Modifier;

@Retention (RetentionPolicy.RUNTIME)
@Repeatable (Access.Accesses.class)
@Target ({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
public @interface Access {
	/**
	 * @return the access flags of this class on the given platform
	 */
	String[] flags();

	Platform[] platforms();

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
	@interface Accesses {
		Access[] value();
	}
}
