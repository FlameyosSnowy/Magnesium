package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.base.MagnesiumController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExceptionHandler {
    Class<? extends MagnesiumController> controllerType() default MagnesiumController.class;
}
