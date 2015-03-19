package org.jooby;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;

/**
 * A {@link Result} builder with some utility static methods (nothing more).
 *
 * @author edgar
 * @since 0.5.0
 */
public class Results {

  /**
   * Set the result
   *
   * @param entity A result value.
   * @return A new result.
   */
  public static @Nonnull Result with(final @Nonnull Object entity) {
    return new Result().set(entity);
  }

  /**
   * Set the result
   *
   * @param entity A result value.
   * @param status A HTTP status.
   * @return A new result.
   */
  public static @Nonnull Result with(final @Nonnull Object entity, final Status status) {
    return new Result().status(status).set(entity);
  }

  /**
   * Set the result
   *
   * @param entity A result value.
   * @param status A HTTP status.
   * @return A new result.
   */
  public static @Nonnull Result with(final @Nonnull Object entity, final int status) {
    return new Result().status(status).set(entity);
  }

  /**
   * Set the response status.
   *
   * @param status A status!
   * @return A new result.
   */
  public static @Nonnull Result with(final @Nonnull Status status) {
    requireNonNull(status, "A HTTP status is required.");
    return new Result().status(status);
  }

  /**
   * Set the response status.
   *
   * @param status A status!
   * @return A new result.
   */
  public static @Nonnull Result with(final int status) {
    requireNonNull(status, "A HTTP status is required.");
    return new Result().status(status);
  }

  /**
   * @return A new result with {@link Status#OK}.
   */
  public static @Nonnull Result ok() {
    return with(Status.OK);
  }

  /**
   * @param entity A result content!
   * @return A new result with {@link Status#OK} and given content.
   */
  public static @Nonnull Result ok(final @Nonnull Object entity) {
    return ok().set(entity);
  }

  /**
   * @return A new result with {@link Status#ACCEPTED}.
   */
  public static @Nonnull Result accepted() {
    return with(Status.ACCEPTED);
  }

  /**
   * @param content A result content!
   * @return A new result with {@link Status#ACCEPTED}.
   */
  public static @Nonnull Result accepted(final @Nonnull Object content) {
    return accepted().set(content);
  }

  /**
   * @return A new result with {@link Status#NO_CONTENT}.
   */
  public static @Nonnull Result noContent() {
    return with(Status.NO_CONTENT);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new result.
   */
  public static @Nonnull Result redirect(final @Nonnull String location) {
    return redirect(Status.FOUND, location);
  }

  /**
   * Produces a redirect (307) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new result.
   */
  public static @Nonnull Result tempRedirect(final @Nonnull String location) {
    return redirect(Status.TEMPORARY_REDIRECT, location);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new result.
   */
  public static @Nonnull Result moved(final @Nonnull String location) {
    return redirect(Status.MOVED_PERMANENTLY, location);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param location A location.
   * @return A new result.
   */
  public static @Nonnull Result seeOther(final @Nonnull String location) {
    return redirect(Status.SEE_OTHER, location);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param status A HTTP redirect status.
   * @param location A location.
   * @return A new result.
   */
  private static Result redirect(final Status status, final String location) {
    requireNonNull(location, "A location is required.");
    return with(status).header("location", location);
  }

}
