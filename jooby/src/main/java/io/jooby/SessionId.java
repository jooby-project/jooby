/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Find, save and delete a session ID (cookie or header) into/from the web {@link Context}.
 *
 * @author edgar
 */
public interface SessionId {

  /**
   * Find session ID.
   *
   * @param ctx Web context.
   * @return Session ID or <code>null</code>.
   */
  @Nullable String findSessionId(@Nonnull Context ctx);

  /**
   * Save session ID in the web context.
   *
   * @param ctx Web context.
   * @param sessionId Session ID to save.
   */
  void saveSessionId(@Nonnull Context ctx, @Nonnull String sessionId);

  /**
   * Delete session ID in the web context.
   *
   * @param ctx Web context.
   * @param sessionId Session ID to save.
   */
  void deleteSessionId(@Nonnull Context ctx, @Nonnull String sessionId);

  /**
   * Create a cookie-based Session ID.
   *
   * @param cookie Cookie template.
   * @return Session ID.
   */
  static @Nonnull SessionId cookie(@Nonnull Cookie cookie) {
    String name = cookie.getName();
    return new SessionId() {
      @Override public String findSessionId(Context ctx) {
        return ctx.cookieMap().get(name);
      }

      @Override public void saveSessionId(Context ctx, String sessionId) {
        String existingId = findSessionId(ctx);
        // write cookie for new or expiring session
        if (existingId == null || cookie.getMaxAge() > 0) {
          ctx.setResponseCookie(cookie.clone().setValue(sessionId));
        }
      }

      @Override public void deleteSessionId(Context ctx, String sessionId) {
        ctx.setResponseCookie(cookie.clone().setMaxAge(0));
      }
    };
  }

  /**
   * Create a header-based Session ID.
   *
   * @param name Header name.
   * @return Session ID.
   */
  static @Nonnull SessionId header(@Nonnull String name) {
    return new SessionId() {
      @Override public String findSessionId(Context ctx) {
        return ctx.headerMap().get(name);
      }

      @Override public void saveSessionId(Context ctx, String sessionId) {
        ctx.setResponseHeader(name, sessionId);
      }

      @Override public void deleteSessionId(Context ctx, String sessionId) {
        ctx.removeResponseHeader(name);
      }
    };
  }
}
