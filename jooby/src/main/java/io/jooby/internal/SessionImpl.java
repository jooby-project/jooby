package io.jooby.internal;

import io.jooby.Session;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionImpl implements Session {

  private String id;

  private ConcurrentHashMap<String, String> attributes = new ConcurrentHashMap<>();

  private Instant creationTime;

  private Instant lastAccessedTime;

  public SessionImpl(String id, Instant creationTime) {
    this.id = id;
    this.creationTime = creationTime;
    this.lastAccessedTime = creationTime;
  }

  @Override public @Nonnull String getId() {
    return id;
  }

  @Override public @Nonnull Value get(@Nonnull String name) {
    String value = attributes.get(name);
    return value == null ? Value.missing(name) : Value.value(name, value);
  }

  @Override public @Nonnull Session put(@Nonnull String name, int value) {
    attributes.put(name, Integer.toString(value));
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, long value) {
    attributes.put(name, Long.toString(value));
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, CharSequence value) {
    attributes.put(name, value.toString());
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, String value) {
    attributes.put(name, value);
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, float value) {
    attributes.put(name, Float.toString(value));
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, double value) {
    attributes.put(name, Double.toString(value));
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, boolean value) {
    attributes.put(name, Boolean.toString(value));
    return this;
  }

  @Override public @Nonnull Session put(@Nonnull String name, Number value) {
    attributes.put(name, value.toString());
    return this;
  }

  @Override public @Nonnull Value remove(@Nonnull String name) {
    String value = attributes.remove(name);
    return value == null ? Value.missing(name) : Value.value(name, value);
  }

  @Override public @Nonnull Map<String, String> toMap() {
    return Collections.unmodifiableMap(attributes);
  }

  @Override public @Nonnull Instant getCreationTime() {
    return creationTime;
  }

  @Override public @Nonnull Instant getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override public @Nonnull Duration getMaxInactiveInterval() {
    return Duration.between(creationTime, lastAccessedTime);
  }

  @Override public @Nonnull Session setLastAccessedTime(@Nonnull Instant lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override public void destroy() {
    attributes.clear();
  }
}
