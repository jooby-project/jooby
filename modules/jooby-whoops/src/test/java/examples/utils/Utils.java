/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.utils;

public class Utils {
  public static Object fail(Object id) {
    throw new IllegalArgumentException("Something Broke!");
  }
}
