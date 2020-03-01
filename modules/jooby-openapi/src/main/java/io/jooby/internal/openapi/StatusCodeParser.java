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
