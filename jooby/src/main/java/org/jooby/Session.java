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

/**
 * <p>
 * Sessions are created on demand via: {@link Request#session()}.
 * </p>
 *
 * <p>
 * Sessions have a lot of uses cases but most commons are: auth, store information about current
 * user, etc.
 * </p>
 *
 * <p>
 * A session attribute must be {@link String} or a primitive. Session doesn't allow to store
 * arbitrary objects. It is a simple mechanism to store basic data.
 * </p>
 *
 * <h1>Session configuration</h1>
 *
 * <h2>No timeout</h2>
 * <p>
 * There is no timeout for sessions from server perspective. By default, a session will expire when
 * the user close the browser (a.k.a session cookie).
 * </p>
 *
 * <h2>Session store</h2>
 * <p>
 * A {@link Session.Store} is responsible for saving session data. Sessions are kept in memory, by
 * default using the {@link Session.Mem} store, which is useful for development, but wont scale well
 * on production environments. An redis, memcached, ehcache store will be a better option.
 * </p>
 *
 * <h3>Store life-cycle</h3>
 * <p>
 * Sessions are persisted every time a request exit, if they are dirty. A session get dirty if an
 * attribute is added or removed from it.
 * </p>
 * <p>
 * The <code>session.saveInterval</code> property indicates how frequently a session will be
 * persisted (in millis).
 * </p>
 * <p>
 * In short, a session is persisted when: 1) it is dirty; or 2) save interval has expired it.
 * </p>
 *
 * <h1>Cookie configuration</h1>
 * <p>
 * Next session describe the most important options:
 * </p>
 *
 * <h2>max-age</h2>
 * <p>
 * The <code>session.cookie.maxAge</code> sets the maximum age in seconds. A positive value
 * indicates that the cookie will expire after that many seconds have passed. Note that the value is
 * the <i>maximum</i> age when the cookie will expire, not the cookie's current age.
 *
 * A negative value means that the cookie is not stored persistently and will be deleted when the
 * Web browser exits.
 *
 * Default maxAge is: <code>-1</code>.
 *
 * </p>
 *
 * <h2>signed cookie</h2>
 * <p>
 * If the <code>application.secret</code> property has been set, then the session cookie will be
 * signed it with it.
 * </p>
 *
 * <h2>cookie's name</h2>
 * <p>
 * The <code>session.cookie.name</code> indicates the name of the cookie that hold the session ID,
 * by defaults: <code>jooby.sid</code>. Cookie's name can be explicitly set with
 * {@link Cookie.Definition#name(String)} on {@link Session.Definition#cookie()}.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Session {

  /**
   * Hold session related configuration parameters.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Definition {

    /** Session store. */
    private Object store;

    /** Session cookie. */
    private Cookie.Definition cookie;

    /** Save interval. */
    private Long saveInterval;

    /**
     * Creates a new session definition.
     *
     * @param store A session store.
     */
    public Definition(final Class<? extends Store> store) {
      this.store = requireNonNull(store, "A session store is required.");
      cookie = new Cookie.Definition();
    }

    /**
     * Creates a new session definition.
     *
     * @param store A session store.
     */
    public Definition(final Store store) {
      this.store = requireNonNull(store, "A session store is required.");
      cookie = new Cookie.Definition();
    }

    /**
     * Indicates how frequently a no-dirty session should be persisted (in millis).
     *
     * @return A save interval that indicates how frequently no dirty session should be persisted.
     */
    public Optional<Long> saveInterval() {
      return Optional.ofNullable(saveInterval);
    }

    /**
     * Set/override how frequently a no-dirty session should be persisted (in millis).
     *
     * @param saveInterval Save interval in millis or <code>-1</code> for turning it off.
     * @return This definition.
     */
    public Definition saveInterval(final long saveInterval) {
      this.saveInterval = saveInterval;
      return this;
    }

    /**
     * @return A session store instance or class.
     */
    public Object store() {
      return store;
    }

    /**
     * @return Configure cookie session.
     */
    public Cookie.Definition cookie() {
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
    Session get(Session.Builder builder);

    /**
     * Save/persist a session.
     *
     * @param session A session to be persisted.
     */
    void save(Session session);

    void create(final Session session);

    /**
     * Delete a session by ID.
     *
     * @param id A session ID.
     */
    void delete(String id);

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
  class Mem implements Store {

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
    Builder set(final String name, final String value);

    /**
     * Set one ore more session local attributes.
     *
     * @param attributes Attributes to add.
     * @return This builder.
     */
    Builder set(final Map<String, String> attributes);

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

  /**
   * @return Session ID.
   */
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
   * @return A value or empty optional.
   */
  Mutant get(final String name);

  /**
   * @return An immutable copy of local attributes.
   */
  Map<String, String> attributes();

  /**
   * Test if the var name exists inside the session local attributes.
   *
   * @param name A local var's name.
   * @return True, for existing locals.
   */
  default boolean isSet(final String name) {
    return get(name).isPresent();
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final byte value) {
    return set(name, Byte.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final char value) {
    return set(name, Character.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final boolean value) {
    return set(name, Boolean.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final short value) {
    return set(name, Short.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final int value) {
    return set(name, Integer.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final long value) {
    return set(name, Long.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final float value) {
    return set(name, Float.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final double value) {
    return set(name, Double.toString(value));
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  default Session set(final String name, final CharSequence value) {
    return set(name, value.toString());
  }

  /**
   * Set a session local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that null values are NOT allowed.
   *
   * @param name A local's name.
   * @param value A local's value.
   * @return This session.
   */
  Session set(final String name, final String value);

  /**
   * Remove a local value (if any) from session locals.
   *
   * @param name A local var's name.
   * @return Existing value or empty optional.
   */
  Mutant unset(final String name);

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
