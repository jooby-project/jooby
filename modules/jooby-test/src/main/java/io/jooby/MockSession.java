/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mock session.
 */
public class MockSession implements Session {
  private MockContext ctx;
  private String sessionId;

  private Map<String, String> data = new HashMap<>();
  private Instant creationTime;
  private Instant lastAccessedTime;
  private boolean isNew;
  private boolean modified;

  /**
   * Creates a mock session.
   *
   * @param ctx Mock context.
   * @param session Session.
   */
  MockSession(@Nonnull MockContext ctx, @Nonnull MockSession session) {
    this.ctx = ctx.setSession(this);
    this.data = session.data;
    this.isNew = session.isNew;
    this.modified = session.modified;
    this.sessionId = session.sessionId;
    this.creationTime = session.creationTime;
    this.lastAccessedTime = session.lastAccessedTime;
  }

  /**
   * Creates a mock session.
   *
   * @param ctx Mock context.
   * @param sessionId Session ID.
   */
  public MockSession(@Nonnull MockContext ctx, @Nonnull String sessionId) {
    this.ctx = ctx.setSession(this);
    this.sessionId = sessionId;
    this.creationTime = Instant.now();
    this.lastAccessedTime = Instant.now();
  }

  /**
   * Mock session with a random ID.
   *
   * @param ctx Mock context.
   */
  public MockSession(@Nonnull MockContext ctx) {
    this(ctx, UUID.randomUUID().toString());
  }

  /**
   * Mock session with a random ID. Useful for creating a shared session between a mock router
   * instance.
   */
  public MockSession() {
    this.sessionId = UUID.randomUUID().toString();
    this.creationTime = Instant.now();
    this.lastAccessedTime = Instant.now();
  }

  @Nonnull @Override public String getId() {
    return sessionId;
  }

  @Nonnull @Override public MockSession setId(@Nullable String id) {
    this.sessionId = id;
    return this;
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return Optional.ofNullable(data.get(name))
        .map(value -> Value.create(ctx, name, value))
        .orElse(Value.missing(name));
  }

  @Nonnull @Override public Session put(@Nonnull String name, @Nonnull String value) {
    data.put(name, value);
    return this;
  }

  @Nonnull @Override public Value remove(@Nonnull String name) {
    Value value = get(name);
    data.remove(name);
    return value;
  }

  @Nonnull @Override public Map<String, String> toMap() {
    return data;
  }

  @Nonnull @Override public Instant getCreationTime() {
    return creationTime;
  }

  @Nonnull @Override public Session setCreationTime(@Nonnull Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Nonnull @Override public Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Nonnull @Override public Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override public boolean isNew() {
    return isNew;
  }

  @Nonnull @Override public Session setNew(boolean isNew) {
    this.isNew = isNew;
    return this;
  }

  @Override public boolean isModify() {
    return modified;
  }

  @Nonnull @Override public Session setModify(boolean modify) {
    this.modified = modify;
    return this;
  }

  @Override public Session clear() {
    data.clear();
    return this;
  }

  @Override public Session renewId() {
    return this;
  }

  @Override public void destroy() {
    clear();
  }
}
