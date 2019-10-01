/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.SessionImpl;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;

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
   * Session ID.
   *
   * @return Session ID.
   */
  @Nonnull String getId();

  /**
   * Get a session attribute.
   *
   * @param name Attribute's name.
   * @return An attribute value or missing value.
   */
  @Nonnull ValueNode get(@Nonnull String name);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, int value) {
    return put(name, Integer.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, long value) {
    return put(name, Long.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, @Nonnull CharSequence value) {
    return put(name, value.toString());
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, @Nonnull String value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, float value) {
    return put(name, Float.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, double value) {
    return put(name, Double.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, boolean value) {
    return put(name, Boolean.toString(value));
  }

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  default @Nonnull Session put(@Nonnull String name, @Nonnull Number value) {
    return put(name, value.toString());
  }

  /**
   * Remove a session attribute.
   *
   * @param name Attribute's name.
   * @return Session attribute or missing value.
   */
  @Nonnull ValueNode remove(@Nonnull String name);

  /**
   * Read-only copy of session attributes.
   *
   * @return Read-only attributes.
   */
  @Nonnull Map<String, String> toMap();

  /**
   * Session creation time.
   *
   * @return Session creation time.
   */
  @Nonnull Instant getCreationTime();

  /**
   * Set session creation time.
   *
   * @param creationTime Session creation time.
   * @return This session.
   */
  @Nonnull Session setCreationTime(@Nonnull Instant creationTime);

  /**
   * Session last accessed time.
   *
   * @return Session creation time.
   */
  @Nonnull Instant getLastAccessedTime();

  /**
   * Set session last accessed time.
   *
   * @param lastAccessedTime Session creation time.
   * @return This session.
   */
  @Nonnull Session setLastAccessedTime(@Nonnull Instant lastAccessedTime);

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
  @Nonnull Session setNew(boolean isNew);

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
  @Nonnull Session setModify(boolean modify);

  /**
   * Remove all attributes.
   *
   * @return This session.
   */
  Session clear();

  /**
   * Destroy/invalidates this session.
   */
  void destroy();

  /**
   * Creates a new session.
   *
   * @param ctx Web context.
   * @param id Session ID.
   * @return A new session.
   */
  static @Nonnull Session create(@Nonnull Context ctx, @Nonnull String id) {
    return new SessionImpl(ctx, id);
  }

  /**
   * Creates a new session.
   *
   * @param ctx Web context.
   * @param id Session ID.
   * @param data Session attributes.
   * @return A new session.
   */
  static @Nonnull Session create(@Nonnull Context ctx, @Nonnull String id,
      @Nonnull Map<String, String> data) {
    return new SessionImpl(ctx, id, data);
  }
}
