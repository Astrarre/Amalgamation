package net.devtech.testbytecodemerge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * a sided platform
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Platform.Platforms.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
public @interface Platform {
	/**
	 * @return the names of the platform. eg. ["fabric", "client"] or ["spigot"]
	 */
	String[] value();

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
	@interface Platforms {
		Platform[] value();
	}
}
