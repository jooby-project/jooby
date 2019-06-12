/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Session;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionImpl implements Session {

  private boolean isNew;

  private String id;

  private ConcurrentHashMap<String, String> attributes = new ConcurrentHashMap<>();

  private Instant creationTime;

  private Instant lastAccessedTime;

  private boolean modify;

  public SessionImpl(String id) {
    this.id = id;
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

  @Override public @Nonnull Value get(@Nonnull String name) {
    return Value.create(name, attributes.get(name));
  }

  @Override public @Nonnull Session put(@Nonnull String name, String value) {
    attributes.put(name, value);
    updateFlags();
    return this;
  }

  @Override public @Nonnull Value remove(@Nonnull String name) {
    String value = attributes.remove(name);
    updateFlags();
    return value == null ? Value.missing(name) : Value.value(name, value);
  }

  @Override public @Nonnull Map<String, String> toMap() {
    return Collections.unmodifiableMap(attributes);
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
    updateFlags();
    return this;
  }

  @Override public void destroy() {
    attributes.clear();
  }

  private void updateFlags() {
    modify = true;
    lastAccessedTime = Instant.now();
  }
}
