/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
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
    super((message + "\nFor more details, please visit: https://jooby.io/usage#" + id));
  }

  /**
   * Creates a mvc route missing exception.
   *
   * @param mvcRoute Mvc route.
   * @return Usage exception.
   */
  public static @Nonnull Usage mvcRouterNotFound(@Nonnull Class mvcRoute) {
    return apt("Router not found: `" + mvcRoute.getName()
        + "`. Make sure Jooby annotation processor is configured properly.", "router-not-found");
  }

  private static Usage apt(String message, String id) {
    return new Usage(message, "annotation-processing-tool-" + id);
  }
}
