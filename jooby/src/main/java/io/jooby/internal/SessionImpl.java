/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Session;
import io.jooby.SessionOptions;
import io.jooby.SessionStore;
import io.jooby.Value;
import io.jooby.ValueNode;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionImpl implements Session {

  private Context ctx;

  private boolean isNew;

  private String id;

  private Map<String, String> attributes;

  private Instant creationTime;

  private Instant lastAccessedTime;

  private boolean modify;

  public SessionImpl(Context ctx, String id) {
    this(ctx, id, new ConcurrentHashMap<>());
  }

  public SessionImpl(Context ctx, String id, Map<String, String> attributes) {
    this.ctx = ctx;
    this.id = id;
    this.attributes = attributes;
  }

  @Override public boolean isNew() {
    return isNew;
  }

  @Nonnull @Override public Session setNew(boolean aNew) {
    this.isNew = aNew;
    return this;
  }

  @Override public boolean isModify() {
    return modify;
  }

  @Nonnull @Override public Session setModify(boolean modify) {
    this.modify = modify;
    return this;
  }

  @Override public @Nonnull String getId() {
    return id;
  }

  @Override public @Nonnull ValueNode get(@Nonnull String name) {
    return Value.create(ctx, name, attributes.get(name));
  }

  @Override public @Nonnull Session put(@Nonnull String name, String value) {
    attributes.put(name, value);
    updateState();
    return this;
  }

  @Override public @Nonnull ValueNode remove(@Nonnull String name) {
    String value = attributes.remove(name);
    updateState();
    return value == null ? Value.missing(name) : Value.value(ctx, name, value);
  }

  @Override public @Nonnull Map<String, String> toMap() {
    return attributes;
  }

  @Override public @Nonnull Instant getCreationTime() {
    return creationTime;
  }

  @Nonnull @Override public Session setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Override public @Nonnull Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override public @Nonnull Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override public Session clear() {
    attributes.clear();
    updateState();
    return this;
  }

  @Override public void destroy() {
    ctx.getAttributes().remove(NAME);
    attributes.clear();
    SessionStore store = store(ctx);
    store.getSessionToken().deleteToken(ctx, id);
    store.deleteSession(ctx);
  }

  private void updateState() {
    modify = true;
    lastAccessedTime = Instant.now();
    store(ctx).getSessionToken().saveToken(ctx, id);
  }

  private static SessionOptions options(Context ctx) {
    Router router = ctx.getRouter();
    return router.getSessionOptions();
  }

  private static SessionStore store(Context ctx) {
    return options(ctx).getStore();
  }

}
