package io.jooby;

import javax.annotation.Nonnull;

/**
 * Usage exceptions. They provide a descriptive message with a link for a detailed section.
 *
 * @since 2.1.0
 */
public class Usage extends RuntimeException {
  /**
   * Creates a new Usage exception.
   *
   * @param message Message.
   * @param id Link to detailed section.
   */
  public Usage(@Nonnull String message, @Nonnull String id) {
    super((message + "\nFor more details, please visit: https://io.jooby/usage#" + id));
  }

  /**
   * Creates a mvc route missing exception.
   *
   * @param mvcRoute Mvc route.
   * @return Usage exception.
   */
  public static @Nonnull Usage mvcRouteMissing(@Nonnull Class mvcRoute) {
    return new Usage("Mvc route not found: `" + mvcRoute.getName()
        + "`. Make sure Jooby annotation processor is configured properly.", "mvc-route-apt");
  }
}
