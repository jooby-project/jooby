package org.jooby;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jooby.Jooby;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface App {
  Class<? extends Jooby> value() default Jooby.class;
}
