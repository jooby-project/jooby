/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.SessionId;

public class SecretSessionId implements SessionId {

  private SessionId sessionId;

  private final String secret;

  public SecretSessionId(SessionId sessionId, String secret) {
    this.sessionId = sessionId;
    this.secret = secret;
  }

  @Override public String findSessionId(Context ctx) {
    String sessionId = this.sessionId.findSessionId(ctx);
    return sessionId == null ? null : Cookie.unsign(sessionId, secret);
  }

  @Override public void saveSessionId(Context ctx, String sessionId) {
    this.sessionId.saveSessionId(ctx, Cookie.sign(sessionId, secret));
  }

  @Override public void deleteSessionId(Context ctx, String sessionId) {
    this.sessionId.deleteSessionId(ctx, sessionId);
  }
}
