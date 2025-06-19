/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Session;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

/** Mock session. */
public class MockSession implements Session {
  private MockContext ctx;
  private String sessionId;
  private ValueFactory valueFactory = new ValueFactory();

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
  MockSession(@NonNull MockContext ctx, @NonNull MockSession session) {
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
  public MockSession(@NonNull MockContext ctx, @NonNull String sessionId) {
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
  public MockSession(@NonNull MockContext ctx) {
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

  @NonNull @Override
  public String getId() {
    return sessionId;
  }

  @NonNull @Override
  public MockSession setId(@Nullable String id) {
    this.sessionId = id;
    return this;
  }

  @NonNull @Override
  public Value get(@NonNull String name) {
    return Optional.ofNullable(data.get(name))
        .map(value -> Value.create(valueFactory, name, value))
        .orElse(Value.missing(name));
  }

  @NonNull @Override
  public Session put(@NonNull String name, @NonNull String value) {
    data.put(name, value);
    return this;
  }

  @NonNull @Override
  public Value remove(@NonNull String name) {
    Value value = get(name);
    data.remove(name);
    return value;
  }

  @NonNull @Override
  public Map<String, String> toMap() {
    return data;
  }

  @NonNull @Override
  public Instant getCreationTime() {
    return creationTime;
  }

  @NonNull @Override
  public Session setCreationTime(@NonNull Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @NonNull @Override
  public Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @NonNull @Override
  public Session setLastAccessedTime(@NonNull Instant lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @NonNull @Override
  public Session setNew(boolean isNew) {
    this.isNew = isNew;
    return this;
  }

  @Override
  public boolean isModify() {
    return modified;
  }

  @NonNull @Override
  public Session setModify(boolean modify) {
    this.modified = modify;
    return this;
  }

  @Override
  public Session clear() {
    data.clear();
    return this;
  }

  @Override
  public Session renewId() {
    return this;
  }

  @Override
  public void destroy() {
    clear();
  }
}
