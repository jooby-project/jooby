/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Find, save and delete a session token (cookie or header) into/from the web {@link Context}.
 *
 * @author edgar
 */
public interface SessionToken {

  class CookieID implements SessionToken {

    private final Cookie cookie;

    public CookieID(@Nonnull Cookie cookie) {
      this.cookie = cookie;
    }

    @Nullable @Override public String findToken(@Nonnull Context ctx) {
      return ctx.cookieMap().get(cookie.getName());
    }

    @Override public void saveToken(@Nonnull Context ctx, @Nonnull String token) {
      String existingId = findToken(ctx);
      // write cookie for new or expiring session
      if (existingId == null || cookie.getMaxAge() > 0) {
        ctx.setResponseCookie(cookie.clone().setValue(token));
      }
    }

    @Override public void deleteToken(@Nonnull Context ctx, @Nonnull String token) {
      ctx.setResponseCookie(cookie.clone().setMaxAge(0));
    }
  }

  class HeaderID implements SessionToken {

    private final String name;

    public HeaderID(String name) {
      this.name = name;
    }

    @Nullable @Override public String findToken(@Nonnull Context ctx) {
      return ctx.headerMap().get(name);
    }

    @Override public void saveToken(@Nonnull Context ctx, @Nonnull String token) {
      ctx.setResponseHeader(name, token);
    }

    @Override public void deleteToken(@Nonnull Context ctx, @Nonnull String token) {
      ctx.removeResponseHeader(name);
    }
  }

  /**
   * Find session ID.
   *
   * @param ctx Web context.
   * @return Session ID or <code>null</code>.
   */
  @Nullable String findToken(@Nonnull Context ctx);

  /**
   * Save session ID in the web context.
   *
   * @param ctx Web context.
   * @param token
   */
  void saveToken(@Nonnull Context ctx, @Nonnull String token);

  /**
   * Delete session ID in the web context.
   *
   * @param ctx Web context.
   * @param token Session ID to save.
   */
  void deleteToken(@Nonnull Context ctx, @Nonnull String token);

  /**
   * Create a cookie-based Session ID.
   *
   * @param cookie Cookie template.
   * @return Session ID.
   */
  static @Nonnull SessionToken cookie(@Nonnull Cookie cookie) {
    return new CookieID(cookie);
  }

  /**
   * Create a header-based Session ID.
   *
   * @param name Header name.
   * @return Session ID.
   */
  static @Nonnull SessionToken header(@Nonnull String name) {
    return new HeaderID(name);
  }
}
