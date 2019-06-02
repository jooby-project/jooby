/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

public interface SessionId {

  String findSessionId(Context ctx);

  void saveSessionId(Context ctx, String sessionId);

  void deleteSessionId(Context ctx, String sessionId);

  static SessionId cookie(Cookie cookie) {
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

  static SessionId header(String name) {
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
