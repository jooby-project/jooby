/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

public class StatusCodeParser {
  public static int statusCode(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException x) {
      return -1;
    }
  }

  public static boolean isSuccessCode(String value) {
    int code = statusCode(value);
    return code > 0 && code < 400;
  }
}
