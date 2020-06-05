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

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MemorySessionStore extends SessionStore.InMemory {

  private ConcurrentHashMap<String, Data> sessions = new ConcurrentHashMap<>();

  private Duration timeout;

  public MemorySessionStore(SessionToken token, Duration timeout) {
    super(token);
    this.timeout = Optional.ofNullable(timeout)
        .filter(t -> t.toMillis() > 0)
        .orElse(null);
  }

  @Override protected Data getOrCreate(String sessionId,
      Function<String, Data> factory) {
    return sessions.computeIfAbsent(sessionId, factory);
  }

  @Override protected Data getOrNull(String sessionId) {
    return sessions.get(sessionId);
  }

  @Override protected Data remove(String sessionId) {
    return sessions.remove(sessionId);
  }

  @Override protected void put(String sessionId, Data data) {
    sessions.put(sessionId, data);
  }

  @Override public Session findSession(Context ctx) {
    purge();
    return super.findSession(ctx);
  }

  /**
   * Check for expired session and delete them.
   */
  private void purge() {
    if (timeout != null) {
      Iterator<Map.Entry<String, Data>> iterator = sessions.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Data> entry = iterator.next();
        Data session = entry.getValue();
        if (session.isExpired(timeout)) {
          iterator.remove();
        }
      }
    }
  }

  public SessionStore setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

}
