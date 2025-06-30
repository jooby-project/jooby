/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.security.SecureRandom;
import java.util.Base64;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.internal.MultipleSessionToken;

/**
 * Find, save and delete a session token (cookie, header, parameter, etc) into/from the web {@link
 * Context}.
 *
 * @author edgar
 */
public interface SessionToken {

  /**
   * Looks for a session ID from request cookie headers. This strategy:
   *
   * <p>- find a token from a request cookie. - on save, set a response cookie on new sessions or
   * when cookie has a max-age value. - on destroy, expire the cookie.
   */
  class CookieID implements SessionToken {

    private final Cookie cookie;

    /**
     * Creates a Cookie ID.
     *
     * @param cookie Cookie to use.
     */
    public CookieID(@NonNull Cookie cookie) {
      this.cookie = cookie;
    }

    @Nullable @Override
    public String findToken(@NonNull Context ctx) {
      return ctx.cookieMap().get(cookie.getName());
    }

    @Override
    public void saveToken(@NonNull Context ctx, @NonNull String token) {
      ctx.setResponseCookie(cookie.clone().setValue(token));
    }

    @Override
    public void deleteToken(@NonNull Context ctx, @NonNull String token) {
      ctx.setResponseCookie(cookie.clone().setValue(token).setMaxAge(0));
    }
  }

  /**
   * Looks for a session ID from request headers. This strategy:
   *
   * <p>- find a token from a request header. - on save, send the header back as response header. -
   * on session destruction. don't send response header back.
   */
  class HeaderID implements SessionToken {

    private final String name;

    /**
     * Creates a new Header ID.
     *
     * @param name Header's name.
     */
    public HeaderID(@NonNull String name) {
      this.name = name;
    }

    @Nullable @Override
    public String findToken(@NonNull Context ctx) {
      return ctx.headerMap().get(name);
    }

    @Override
    public void saveToken(@NonNull Context ctx, @NonNull String token) {
      ctx.setResponseHeader(name, token);
    }

    @Override
    public void deleteToken(@NonNull Context ctx, @NonNull String token) {
      ctx.removeResponseHeader(name);
    }
  }

  /**
   * Looks for a session token from request cookie. This strategy:
   *
   * <p>- find a token from a request cookie. - on save, set a response cookie. - on destroy, expire
   * the cookie.
   */
  class SignedCookie implements SessionToken {

    private final Cookie cookie;

    /**
     * Creates a Cookie ID.
     *
     * @param cookie Cookie to use.
     */
    public SignedCookie(@NonNull Cookie cookie) {
      this.cookie = cookie;
    }

    @Nullable @Override
    public String findToken(@NonNull Context ctx) {
      return ctx.cookieMap().get(cookie.getName());
    }

    @Override
    public void saveToken(@NonNull Context ctx, @NonNull String token) {
      ctx.setResponseCookie(cookie.clone().setValue(token));
    }

    @Override
    public void deleteToken(@NonNull Context ctx, @NonNull String token) {
      ctx.setResponseCookie(cookie.clone().setMaxAge(0));
    }
  }

  /** Secure random for default session token generator. */
  SecureRandom RND = new SecureRandom();

  /** Size of default token generator. */
  int ID_SIZE = 30;

  /**
   * Generate a new token. This implementation produces an url encoder ID using a secure random of
   * {@link #ID_SIZE}.
   *
   * @return A new token.
   */
  default @NonNull String newToken() {
    byte[] bytes = new byte[ID_SIZE];
    RND.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Find session ID.
   *
   * @param ctx Web context.
   * @return Session ID or <code>null</code>.
   */
  @Nullable String findToken(@NonNull Context ctx);

  /**
   * Save session ID in the web context.
   *
   * @param ctx Web context.
   * @param token Token/data to save.
   */
  void saveToken(@NonNull Context ctx, @NonNull String token);

  /**
   * Delete session ID in the web context.
   *
   * @param ctx Web context.
   * @param token Token/data to delete.
   */
  void deleteToken(@NonNull Context ctx, @NonNull String token);

  /* **********************************************************************************************
   * Factory methods
   * **********************************************************************************************
   */

  /**
   * Create a cookie-based Session ID. This strategy:
   *
   * <p>- find a token from a request cookie. - on save, set a response cookie on new sessions or
   * when cookie has a max-age value. - on destroy, expire the cookie.
   *
   * @param cookie Cookie to use.
   * @return Session Token.
   */
  static @NonNull SessionToken cookieId(@NonNull Cookie cookie) {
    return new CookieID(cookie);
  }

  /**
   * Create a signed-cookie-based Session token. This strategy:
   *
   * <p>- find a token from a request cookie. - on save, set a response cookie. - on destroy, expire
   * the cookie.
   *
   * @param cookie Cookie to use.
   * @return Session Token.
   */
  static @NonNull SessionToken signedCookie(@NonNull Cookie cookie) {
    return new SignedCookie(cookie);
  }

  /**
   * Create a header-based Session Token. This strategy:
   *
   * <p>- find a token from a request header. - on save, send the header back as response header. -
   * on session destroy. don't send response header back.
   *
   * @param name Header name.
   * @return Session Token.
   */
  static @NonNull SessionToken header(@NonNull String name) {
    return new HeaderID(name);
  }

  /**
   * Combine/compose two or more session tokens. Example:
   *
   * <pre>{@code
   * SessionToken token = SessionToken.combine(
   *     SessionToken.header("TOKEN"),
   *     SessionToken.cookie(SID)
   * );
   * }</pre>
   *
   * On new session, creates a response header and cookie. On save token, generates a response
   * header or cookie based on best matches. On delete token, generates a response header or cookie
   * based on best matches.
   *
   * @param tokens Tokens to use.
   * @return A composed session token.
   */
  static @NonNull SessionToken combine(@NonNull SessionToken... tokens) {
    return new MultipleSessionToken(tokens);
  }
}
