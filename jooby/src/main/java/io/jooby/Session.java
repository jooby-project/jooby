/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.RequestSession;
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
  @Nonnull Value get(@Nonnull String name);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, int value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, long value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, CharSequence value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, String value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, float value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, double value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, boolean value);

  /**
   * Put a session attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This session.
   */
  @Nonnull Session put(@Nonnull String name, Number value);

  /**
   * Remove a session attribute.
   *
   * @param name Attribute's name.
   * @return Session attribute or missing value.
   */
  @Nonnull Value remove(@Nonnull String name);

  /**
   * Read-only attributes.
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
   */
  Session clear();

  /**
   * Destroy/invalidates this session.
   */
  void destroy();

  /**
   * Creates a new session.
   *
   * @param id Session ID.
   * @return A new session.
   */
  static @Nonnull Session create(@Nonnull String id) {
    return new SessionImpl(id);
  }

  /**
   * Creates a new session and attach it to current request.
   *
   * @param context Web context.
   * @param session HTTP session.
   * @return A request attached session.
   */
  static @Nonnull Session create(@Nonnull Context context, @Nonnull Session session) {
    return new RequestSession(context, session);
  }
}
