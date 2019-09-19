/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.MultipleSessionToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.time.Duration;

/**
 * Find, save and delete a session token (cookie, header, parameter, etc)
 * into/from the web {@link Context}.
 *
 * @author edgar
 */
public interface SessionToken {

  /**
   * Looks for a session ID from request cookie headers. This strategy:
   *
   * - find a token from a request cookie.
   * - on save, set a response cookie on new sessions or when cookie has a max-age value.
   * - on destroy, expire the cookie.
   */
  class CookieID implements SessionToken {

    private final Cookie cookie;

    /**
     * Creates a Cookie ID.
     *
     * @param cookie Cookie to use.
     */
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

  /**
   * Looks for a session ID from request headers. This strategy:
   *
   * - find a token from a request header.
   * - on save, send the header back as response header.
   * - on session destroy. don't send response header back.
   */
  class HeaderID implements SessionToken {

    private final String name;

    /**
     * Creates a new Header ID.
     *
     * @param name Header's name.
     */
    public HeaderID(@Nonnull String name) {
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
   * Default cookie for cookie based session stores.
   * Uses <code>jooby.sid</code> as name. It never expires, use the root, only for HTTP.
   */
  Cookie SID = new Cookie("jooby.sid")
      .setMaxAge(Duration.ofSeconds(-1))
      .setHttpOnly(true)
      .setPath("/");

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
   * @param token Token/data to save.
   */
  void saveToken(@Nonnull Context ctx, @Nonnull String token);

  /**
   * Delete session ID in the web context.
   *
   * @param ctx Web context.
   * @param token Token/data to delete.
   */
  void deleteToken(@Nonnull Context ctx, @Nonnull String token);

  /* **********************************************************************************************
   * Factory methods
   * **********************************************************************************************
   */

  /**
   * Create a cookie-based Session ID. This strategy:
   *
   * - find a token from a request cookie.
   * - on save, set a response cookie on new sessions or when cookie has a max-age value.
   * - on destroy, expire the cookie.
   *
   * @param cookie Cookie to use.
   * @return Session Token.
   */
  static @Nonnull SessionToken cookie(@Nonnull Cookie cookie) {
    return new CookieID(cookie);
  }

  /**
   * Create a header-based Session Token. This strategy:
   *
   * - find a token from a request header.
   * - on save, send the header back as response header.
   * - on session destroy. don't send response header back.
   *
   * @param name Header name.
   * @return Session Token.
   */
  static @Nonnull SessionToken header(@Nonnull String name) {
    return new HeaderID(name);
  }

  /**
   * Combine/compose two or more session tokens. Example:
   *
   * <pre>{@code
   *   SessionToken token = SessionToken.combine(
   *       SessionToken.header("TOKEN"),
   *       SessionToken.cookie(SID)
   *   );
   * }
   * </pre>
   *
   * On new session, creates a response header and cookie.
   * On save token, generates a response header or cookie based on best matches.
   * On delete token, generates a response header or cookie based on best matches.
   *
   * @param tokens Tokens to use.
   * @return A composed session token.
   */
  static @Nonnull SessionToken combine(@Nonnull SessionToken... tokens) {
    return new MultipleSessionToken(tokens);
  }
}
