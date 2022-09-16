/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SessionToken;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SignedSessionStore implements SessionStore {

  private final Function<String, Map<String, String>> decoder;

  private final Function<Map<String, String>, String> encoder;

  private final SessionToken token;

  public SignedSessionStore(SessionToken token, Function<String, Map<String, String>> decoder,
      Function<Map<String, String>, String> encoder) {
    this.decoder = decoder;
    this.encoder = encoder;
    this.token = token;
  }

  @NonNull @Override public Session newSession(@NonNull Context ctx) {
    return Session.create(ctx, null).setNew(true);
  }

  @Nullable @Override public Session findSession(@NonNull Context ctx) {
    String signed = token.findToken(ctx);
    if (signed == null) {
      return null;
    }
    Map<String, String> attributes = decoder.apply(signed);
    if (attributes == null || attributes.size() == 0) {
      return null;
    }
    return Session.create(ctx, signed, new HashMap<>(attributes)).setNew(false);
  }

  @Override public void deleteSession(@NonNull Context ctx, @NonNull Session session) {
    token.deleteToken(ctx, null);
  }

  @Override public void touchSession(@NonNull Context ctx, @NonNull Session session) {
    token.saveToken(ctx, encoder.apply(session.toMap()));
  }

  @Override public void saveSession(@NonNull Context ctx, @NonNull Session session) {
    // NOOP
  }

  @Override public void renewSessionId(@NonNull Context ctx, @NonNull Session session) {
    token.saveToken(ctx, encoder.apply(session.toMap()));
  }
}
