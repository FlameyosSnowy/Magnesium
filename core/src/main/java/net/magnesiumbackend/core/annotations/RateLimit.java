package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface RateLimit {
    int     requests()      default 100;
    long    windowSeconds() default 60;
    Algorithm algorithm()   default Algorithm.SLIDING_WINDOW;
    KeyResolverType keyResolver() default KeyResolverType.IP;

    enum Algorithm      { FIXED_WINDOW, SLIDING_WINDOW, TOKEN_BUCKET }
    enum KeyResolverType { IP, USER, API_KEY }
}