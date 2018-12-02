package io.jooby;

public class Err extends RuntimeException {

  public static class Missing extends Err {
    public Missing(String name) {
      super(StatusCode.BAD_REQUEST, "Required value is not present: '" + name + "'");
    }
  }

  public static class BadRequest extends Err {
    public BadRequest(String message) {
      super(StatusCode.BAD_REQUEST, message);
    }

    public BadRequest(String message, Throwable cause) {
      super(StatusCode.BAD_REQUEST, message, cause);
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
    super(message, cause);
    this.statusCode = status;
  }

}
