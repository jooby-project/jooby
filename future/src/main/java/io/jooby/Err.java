package io.jooby;

public class Err extends RuntimeException {

  public static class Missing extends Err {
    public Missing(String name) {
      super(StatusCode.BAD_REQUEST, name);
    }
  }

  public static class TypeMismatch extends Err {
    public TypeMismatch(String message) {
      super(StatusCode.BAD_REQUEST, message);
    }
  }

  public final StatusCode statusCode;

  public Err(StatusCode status) {
    this(status, null);
  }

  public Err(StatusCode status, String message) {
    this(status, message, null);
  }

  public Err(StatusCode status, String message, Throwable cause) {
    super(status + tail(message), cause);
    this.statusCode = status;
  }

  private static String tail(String message) {
    return message == null ? "" : ": " + message;
  }

}
