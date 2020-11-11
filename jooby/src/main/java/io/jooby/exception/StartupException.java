package io.jooby.exception;

/**
 * Thrown when Jooby was unable to initialize and start
 * an application up.
 */
public class StartupException extends RuntimeException {

  /**
   * Creates a new instance of this class with the specified message and cause.
   *
   * @param message The message
   * @param cause The cause
   */
  public StartupException(String message, Throwable cause) {
    super(message, cause);
  }
}
