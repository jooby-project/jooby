/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.value.Value;

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

  @Override
  public Session setNew(boolean aNew) {
    this.isNew = aNew;
    return this;
  }

  @Override
  public boolean isModify() {
    return modify;
  }

  @Override
  public Session setModify(boolean modify) {
    this.modify = modify;
    return this;
  }

  @Override
  public @Nullable String getId() {
    return id;
  }

  @Override
  public Session setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  @Override
  public Value get(String name) {
    return Value.create(ctx.getValueFactory(), name, attributes.get(name));
  }

  @Override
  public Session put(String name, String value) {
    attributes.put(name, value);
    updateState();
    return this;
  }

  public Session put(String name, Object value) {
    attributes.put(name, value.toString());
    return this;
  }

  @Override
  public Value remove(String name) {
    var value = get(name);
    attributes.remove(name);
    updateState();
    return value;
  }

  @Override
  public Map<String, String> toMap() {
    return attributes;
  }

  @Override
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public Session setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Override
  public Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override
  public Session setLastAccessedTime(Instant lastAccessedTime) {
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
