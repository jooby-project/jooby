package io.jooby;

public class Err extends Exception {

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
