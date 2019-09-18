/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Load and save sessions from store (memory, database, etc.).
 *
 * @author edgar
 * @since 2.0.0
 */
public interface SessionStore {

  /**
   * Creates a new session. This method must:
   *
   * - Set session as new {@link Session#setNew(boolean)}
   * - Set session creation time {@link Session#setCreationTime(Instant)}
   * - Set session last accessed time {@link Session#setLastAccessedTime(Instant)}
   *
   * @param id Session ID.
   * @return A new session.
   */
  @Nonnull Session newSession(@Nonnull Context ctx);

  /**
   * Find an existing session by ID. For existing session this method must:
   *
   * - Retrieve/restore session creation time
   * - Set session last accessed time {@link Session#setLastAccessedTime(Instant)}
   *
   * @param id Session ID.
   * @return An existing session or <code>null</code>.
   */
  @Nullable Session findSession(@Nonnull Context ctx);

  /**
   * Delete a session from store. This method must NOT call {@link Session#destroy()}.
   *
   * @param id Session ID.
   */
  void deleteSession(@Nonnull Context ctx);

  /**
   * Save a session. This method must save:
   *
   * - Session attributes/data
   * - Session metadata like: creationTime, lastAccessed time, etc.
   *
   * @param session Session to save.
   */
  void save(@Nonnull Context ctx);
}
