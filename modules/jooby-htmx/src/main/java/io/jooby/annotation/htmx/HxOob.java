/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.htmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an additional template to be rendered and streamed as an Out-of-Band (OOB) swap.
 *
 * <p>Multiple {@code @HxOob} annotations can be applied to a single method. The generated encoder
 * will stream the primary view and all OOB views sequentially. Note that the method's return value
 * must provide the necessary model data for all views.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(HxOobs.class)
public @interface HxOob {
  /**
   * The classpath location of the template file to render as an OOB swap.
   *
   * @return The template path.
   */
  String value();
}
