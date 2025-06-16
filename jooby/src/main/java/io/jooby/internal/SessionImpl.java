/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.Value;
import io.jooby.ValueNode;

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

  @Override
  public boolean isNew() {
    return isNew;
  }

  @NonNull @Override
  public Session setNew(boolean aNew) {
    this.isNew = aNew;
    return this;
  }

  @Override
  public boolean isModify() {
    return modify;
  }

  @NonNull @Override
  public Session setModify(boolean modify) {
    this.modify = modify;
    return this;
  }

  @Override
  public @Nullable String getId() {
    return id;
  }

  @NonNull @Override
  public Session setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  @Override
  public @NonNull Value get(@NonNull String name) {
    return Value.create(ctx.getRouter().getValueFactory(), name, attributes.get(name));
  }

  @Override
  public @NonNull Session put(@NonNull String name, String value) {
    attributes.put(name, value);
    updateState();
    return this;
  }

  @Override
  public @NonNull ValueNode remove(@NonNull String name) {
    String value = attributes.remove(name);
    updateState();
    return value == null
        ? Value.missing(name)
        : Value.value(ctx.getRouter().getValueFactory(), name, value);
  }

  @Override
  public @NonNull Map<String, String> toMap() {
    return attributes;
  }

  @Override
  public @NonNull Instant getCreationTime() {
    return creationTime;
  }

  @NonNull @Override
  public Session setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Override
  public @NonNull Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override
  public @NonNull Session setLastAccessedTime(@NonNull Instant lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override
  public Session clear() {
    attributes.clear();
    updateState();
    return this;
  }

  @Override
  public void destroy() {
    ctx.getAttributes().remove(NAME);
    attributes.clear();
    store(ctx).deleteSession(ctx, this);
  }

  @Override
  public Session renewId() {
    store(ctx).renewSessionId(ctx, this);
    updateState();
    return this;
  }

  private void updateState() {
    modify = true;
    lastAccessedTime = Instant.now();
    store(ctx).touchSession(ctx, this);
  }

  private static SessionStore store(Context ctx) {
    return ctx.getRouter().getSessionStore();
  }
}
