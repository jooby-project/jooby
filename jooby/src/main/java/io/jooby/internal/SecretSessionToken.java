/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.SessionToken;

public class SecretSessionToken implements SessionToken {

  private final SessionToken sessionId;

  private final String secret;

  public SecretSessionToken(SessionToken sessionId, String secret) {
    this.sessionId = sessionId;
    this.secret = secret;
  }

  @Override public String findToken(Context ctx) {
    String sessionId = this.sessionId.findToken(ctx);
    return sessionId == null ? null : Cookie.unsign(sessionId, secret);
  }

  @Override public void saveToken(Context ctx, String token) {
    this.sessionId.saveToken(ctx, Cookie.sign(token, secret));
  }

  @Override public void deleteToken(Context ctx, String token) {
    this.sessionId.deleteToken(ctx, token);
  }
}
