/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sessions are created on demand from {@link Request#session()}.
 *
 * Sessions have a lot of uses cases but most commons are: auth, store information about current
 * user, etc.
 *
 * <h1>Session configuration</h1> <h2>Session timeout</h2>
 * <p>
 * Session timeout is defined by the <code>application.session.timeout</code> property, by default a
 * session will be invalidated after 1800 seconds (30 minutes) of inactivity. Alternative, you can
 * set session timeout from {@link Definition#timeout(long)}.
 * </p>
 * <h2>Session persistence</h2>
 * <p>
 * Session data can be persisted, in order to do that you must provide an implementation of
 * {@link Session.Store}. Sessions are kept in memory, by default.
 * </p>
 * Sessions are persisted every time a request exit if they are dirty. A session get dirty if an
 * attribute is added or removed from it.
 * <p>
 * The <code>application.session.saveInterval</code> property indicates how frequently a session
 * will be persisted. Again, it will be persisted at the time a request exit.
 * </p>
 * <p>
 * In short, a session is persisted when: 1) are dirty; or 2) save interval is expired it.
 * </p>
 * <p>
 * Finally, the <code>application.session.preseverOnStop</code> indicates whenever existing session
 * need to be store at exit time (persisted) or not (invalidated). By default session are preserved
 * on stop.
 * </p>
 *
 * <h1>Cookie configuration</h1>
 * <p>
 * A cookie will be created when a session is created. Cookie is signed using
 * <code>application.secret</code>. For {@link Env dev env} the default secret is set to the
 * location of the Jooby class. For others an <code>application.secret</code> MUST be set, otherwise
 * the application will fail at startup.
 * </p>
 * <p>
 * The <code>application.session.cookie.name</code> indicates the name of the cookie that hold the
 * session ID, by defaults: <code>jooby.sid</code>. Cookie's name can be explicitly set with
 * {@link Cookie.Definition#name(String)} on {@link Session.Definition#cookie()}.
 * </p>
 *
 * <p>
 * The <code>application.session.cookie.maxAge</code> sets the maximum age in seconds. A positive
 * value indicates that the cookie will expire after that many seconds have passed. Note that the
 * value is the <i>maximum</i> age when the cookie will expire, not the cookie's current age.
 *
 * A negative value means that the cookie is not stored persistently and will be deleted when the
 * Web browser exits. A zero value causes the cookie to be deleted.
 *
 * Default maxAge is: <code>-1</code>.
 *
 * Cookie's name can be explicitly set with {@link Cookie.Definition#name(String)} on
 * {@link Session.Definition#cookie()}.
 *
 * <p>
 * A session cookie is marked as secure and httpOnly.
 * </p>
 * <p>
 * Please note that session data is NOT persisted in the cookie, just the session ID. If need to
 * persist a session, see {@link Session.Store}
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Session extends Locals {

  /**
   * Hold session related configuration parameters.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Definition {

    /** Session store. */
    private Store store;

    /** Session timeout . */
    private Long timeout;

    /** Session cookie. */
    private Cookie.Definition cookie;

    /** Save interval. */
    private Long saveInterval;

    /**
     * Creates a new session definition.
     *
     * @param store A session store.
     */
    public Definition(final @Nonnull Store store) {
      this.store = requireNonNull(store, "A session store is required.");
      cookie = new Cookie.Definition();
    }

    /**
     * Set and override default session time out of 30 to something else.
     *
     * @param timeout Session timeout in seconds or <code>-1</code> for no timeout.
     * @return This definition.
     */
    public @Nonnull Definition timeout(final long timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * @return Get session timeout (if any).
     */
    public @Nonnull Optional<Long> timeout() {
      return Optional.ofNullable(timeout);
    }

    /**
     * Indicates in seconds how frequently a no-dirty session should be persisted.
     *
     * @return A save interval that indicates how frequently no dirty session should be persisted.
     */
    public @Nonnull Optional<Long> saveInterval() {
      return Optional.ofNullable(saveInterval);
    }

    /**
     * Set/override how frequently a no-dirty session should be persisted.
     *
     * @param saveInterval Save interval in seconds or <code>-1</code> for turning it off.
     * @return This definition.
     */
    public @Nonnull Definition saveInterval(final long saveInterval) {
      this.saveInterval = saveInterval;
      return this;
    }

    /**
     * @return A session store, defaults to {@link MemoryStore}.
     */
    public @Nonnull Store store() {
      return store;
    }

    /**
     * @return Configure cookie session.
     */
    public @Nonnull Cookie.Definition cookie() {
      return cookie;
    }
  }

  /**
   * Read, save and delete sessions from a persistent storage.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Store {

    /**
     * Get a session by ID (if any).
     *
     * @param builder A session builder.
     * @return A session or <code>null</code>.
     */
    @Nullable
    Session get(@Nonnull Session.Builder builder);

    /**
     * Save/persist a session.
     *
     * @param session A session to be persisted.
     */
    void save(@Nonnull Session session);

    void create(final Session session);

    /**
     * Delete a session by ID.
     *
     * @param id A session ID.
     */
    void delete(@Nonnull String id);

    /**
     * Generate a session ID, default algorithm use an {@link UUID}.
     *
     * @return A unique session ID.
     */
    default String generateID() {
      UUID uuid = UUID.randomUUID();
      return Long.toString(Math.abs(uuid.getMostSignificantBits()), 36)
          + Long.toString(Math.abs(uuid.getLeastSignificantBits()), 36);
    }
  }

  /**
   * A keep in memory session store.
   *
   * @author edgar
   */
  class MemoryStore implements Store {

    private ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

    @Override
    public void create(final Session session) {
      sessions.putIfAbsent(session.id(), session);
    }

    @Override
    public void save(final Session session) {
      sessions.put(session.id(), session);
    }

    @Override
    public Session get(final Session.Builder builder) {
      return sessions.get(builder.sessionId());
    }

    @Override
    public void delete(final String id) {
      sessions.remove(id);
    }

  }

  /**
   * Build or restore a session from a persistent storage.
   *
   * @author edgar
   */
  interface Builder {

    /**
     * @return Session ID.
     */
    String sessionId();

    /**
     * Set a session local attribute.
     *
     * @param name Attribute's name.
     * @param value Attribute's value.
     * @return This builder.
     */
    Builder set(final String name, final Object value);

    /**
     * Set one ore more session local attributes.
     *
     * @param attributes Attributes to add.
     * @return This builder.
     */
    Builder set(final Map<String, Object> attributes);

    /**
     * Set session created date.
     *
     * @param createdAt Session created date.
     * @return This builder.
     */
    Builder createdAt(long createdAt);

    /**
     * Set session last accessed date.
     *
     * @param accessedAt Session last accessed date.
     * @return This builder.
     */
    Builder accessedAt(long accessedAt);

    /**
     * Set session last saved it date.
     *
     * @param savedAt Session last saved it date.
     * @return This builder.
     */
    Builder savedAt(final long savedAt);

    /**
     * Final step to build a new session.
     *
     * @return A session.
     */
    Session build();

  }

  /** Logger logs, man. */
  Logger log = LoggerFactory.getLogger(Session.class);

  /**
   * @return Session ID.
   */
  @Nonnull
  String id();

  /**
   * @return The time when this session was created, measured in milliseconds since midnight January
   *         1, 1970 GMT.
   */
  long createdAt();

  /**
   * @return Last time the session was save it.
   */
  long savedAt();

  /**
   * The last time the client sent a request associated with this session, as the number of
   * milliseconds since midnight January 1, 1970 GMT, and marked by the time the container
   * received the request.
   *
   * <p>
   * Actions that your application takes, such as getting or setting a value associated with the
   * session, do not affect the access time.
   * </p>
   *
   * @return Last time the client sent a request.
   */
  long accessedAt();

  /**
   * @return The time when this session is going to expire, measured in milliseconds since midnight
   *         January 1, 1970 GMT.
   */
  long expiryAt();

  /**
   * Get a object from this session. If the object isn't found this method returns an empty
   * optional.
   *
   * @param name A local var's name.
   * @param <T> Target type.
   * @return A value or empty optional.
   */
  @Override
  @Nonnull
  <T> Optional<T> get(final @Nonnull String name);

  /**
   * @return An immutable copy of local attributes.
   */
  @Override
  @Nonnull
  Map<String, Object> attributes();

  /**
   * Test if the var name exists inside the session local attributes.
   *
   * @param name A local var's name.
   * @return True, for existing locals.
   */
  @Override
  default boolean isSet(final @Nonnull String name) {
    return get(name).isPresent();
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that ONLY none null values are allowed.
   *
   * @param name A local var's name.
   * @param value A local values.
   * @return This session.
   */
  @Override
  @Nonnull Session set(final @Nonnull String name, final @Nonnull Object value);

  /**
   * Remove a local value (if any) from session locals.
   *
   * @param name A local var's name.
   * @param <T> A local type.
   * @return Existing value or empty optional.
   */
  @Override
  <T> Optional<T> unset(final String name);

  /**
   * Unset/remove all the session data.
   *
   * @return This session.
   */
  Session unset();

  /**
   * Invalidates this session then unset any objects bound to it.
   */
  void destroy();

}
