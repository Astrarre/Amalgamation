package io.github.f2bb.amalgamation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Access.Accesses.class)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
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
