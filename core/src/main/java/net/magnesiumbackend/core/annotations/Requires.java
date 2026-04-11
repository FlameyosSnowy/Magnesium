package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.RequiresMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Requires {
    String[] value();

    RequiresMode mode() default RequiresMode.ALL;
}