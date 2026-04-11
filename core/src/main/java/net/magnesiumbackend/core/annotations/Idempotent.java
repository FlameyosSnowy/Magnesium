package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Idempotent {
    /** How long to retain the stored response. Default 24 hours. */
    long ttlHours() default 24;
}