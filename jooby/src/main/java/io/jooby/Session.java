/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.time.Instant;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.internal.SessionImpl;

/**
 * HTTP session. Only basic data types can be saved into session.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Session {
  /** Attribute's name. */
  String NAME = "session";

  /**
   * Session ID or <code>null</code> for stateless (usually signed) sessions.
   *
   * @return Session ID or <code>null</code> for stateless (usually signed) sessions.
   */
  @Nullable String getId();

  /**
   * Set Session ID.
   *
   * @param id Session ID or <code>null</code>
   * @return Session.
   */
  @NonNull Session setId(@Nullable String id);

  /**
   * Get a session attribute.
   *
   * @param name Attribute's name.
   * @return An attribute value or missing value.
   */
  @NonNull Value get(@NonNull String name);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, int value) {
    return put(name, Integer.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, long value) {
    return put(name, Long.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, @NonNull CharSequence value) {
    return put(name, value.toString());
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @NonNull Session put(@NonNull String name, @NonNull String value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, float value) {
    return put(name, Float.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, double value) {
    return put(name, Double.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, boolean value) {
    return put(name, Boolean.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @NonNull Session put(@NonNull String name, @NonNull Number value) {
    return put(name, value.toString());
  }

  /**
   * Remove a session attribute.
   *
   * @param name Attribute's name.
   * @return Session attribute or missing value.
   */
  @NonNull Value remove(@NonNull String name);

  /**
   * Read-only copy of session attributes.
   *
   * @return Read-only attributes.
   */
  @NonNull Map<String, String> toMap();

  /**
   * Session creation time.
   *
   * @return Session creation time.
   */
  @NonNull Instant getCreationTime();

  /**
   * Set session creation time.
   *
   * @param creationTime Session creation time.
   * @return This session.
   */
  @NonNull Session setCreationTime(@NonNull Instant creationTime);

  /**
   * Session last accessed time.
   *
   * @return Session creation time.
   */
  @NonNull Instant getLastAccessedTime();

  /**
   * Set session last accessed time.
   *
   * @param lastAccessedTime Session creation time.
   * @return This session.
   */
  @NonNull Session setLastAccessedTime(@NonNull Instant lastAccessedTime);

  /**
   * True for new sessions.
   *
   * @return True for new sessions.
   */
  boolean isNew();

  /**
   * Set new flag. This method is part of public API but shouldn't be use it.
   *
   * @param isNew New flag.
   * @return This session.
   */
  @NonNull Session setNew(boolean isNew);

  /**
   * True for modified/dirty sessions.
   *
   * @return True for modified/dirty sessions.
   */
  boolean isModify();

  /**
   * Set modify flag. This method is part of public API but shouldn't be use it.
   *
   * @param modify Modify flag.
   * @return This session.
   */
  @NonNull Session setModify(boolean modify);

  /**
   * Remove all attributes.
   *
   * @return This session.
   */
  Session clear();

  /** Destroy/invalidates this session. */
  void destroy();

  /**
   * Assign a new ID to the existing session.
   *
   * @return This session.
   */
  Session renewId();

  /**
   * Creates a new session.
   *
   * @param ctx Web context.
   * @param id Session ID or <code>null</code>.
   * @return A new session.
   */
  static @NonNull Session create(@NonNull Context ctx, @Nullable String id) {
    return new SessionImpl(ctx, id);
  }

  /**
   * Creates a new session.
   *
   * @param ctx Web context.
   * @param id Session ID or <code>null</code>.
   * @param data Session attributes.
   * @return A new session.
   */
  static @NonNull Session create(
      @NonNull Context ctx, @Nullable String id, @NonNull Map<String, String> data) {
    return new SessionImpl(ctx, id, data);
  }
}
