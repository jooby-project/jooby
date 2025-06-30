/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import io.jooby.Extension;
import io.jooby.annotation.Generated;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

public class MvcExtensionGenerator {

  public static Extension toMvcExtension(Object controller) {
    return toMvcExtension(controller.getClass());
  }

  public static Extension toMvcExtension(Class<?> controller) {
    try {
      return new ByteBuddy()
          .subclass(Extension.class)
          .annotateType(
              AnnotationDescription.Builder.ofType(Generated.class)
                  .define("value", controller)
                  .build())
          .make()
          .load(controller.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
          .getLoaded()
          .newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
