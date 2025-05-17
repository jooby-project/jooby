/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.camel;

public class CamelBeans {
  public static String camelBeanId(Class<?> type) {
    String beanId = type.getSimpleName();
    return Character.toLowerCase(beanId.charAt(0)) + beanId.substring(1);
  }
}
