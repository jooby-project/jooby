package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
 * set session timeout from {@link Definition#timeout(int)}.
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
 * <code>application.secret</code>. For {@link Mode dev mode} the default secret is set to the
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
public interface Session {

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
    private Integer timeout;

    /** Save session on stop?. */
    private Boolean preserveOnStop;

    /** Session cookie. */
    private Cookie.Definition cookie;

    /** Save interval. */
    private Integer saveInterval;

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
    public @Nonnull Definition timeout(final int timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * @return Get session timeout (if any).
     */
    public @Nonnull Optional<Integer> timeout() {
      return Optional.ofNullable(timeout);
    }

    /**
     * @return True, when sessions are saved/persisted at shutdown time.
     */
    public @Nonnull Optional<Boolean> preserveOnStop() {
      return Optional.ofNullable(preserveOnStop);
    }

    /**
     * Set/override preserve on stop flag. If true, session will be persisted at shutdown time.
     * Otherwise, a session is invalidated.
     *
     * @param preserveOnStop True, for persisting session on shutdown.
     * @return This definition.
     */
    public @Nonnull Definition preserveOnStop(final boolean preserveOnStop) {
      this.preserveOnStop = preserveOnStop;
      return this;
    }

    /**
     * Indicates in seconds how frequently a no-dirty session should be persisted.
     *
     * @return A save interval that indicates how frequently no dirty session should be persisted.
     */
    public @Nonnull Optional<Integer> saveInterval() {
      return Optional.ofNullable(saveInterval);
    }

    /**
     * Set/override how frequently a no-dirty session should be persisted.
     *
     * @param saveInterval Save interval in seconds or <code>-1</code> for turning it off.
     * @return This definition.
     */
    public @Nonnull Definition saveInterval(final int saveInterval) {
      this.saveInterval = saveInterval;
      return this;
    }

    /**
     * @return A session store, defaults to {@link Store#NOOP}.
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
     * Save reasons.
     *
     * @author edgar
     * @since 0.1.0
     */
    enum SaveReason {
      /**
       * Indicates a save is required bc session is new.
       */
      NEW,

      /**
       * Indicates a save is required bc session is dirty. A session gets dirty when an attribute
       * is added or removed.
       */
      DIRTY,

      /**
       * Indicates a save is required bc a save interval is expired. See
       * {@link Session.Definition#saveInterval()}
       */
      TIME,

      /**
       * Indicates a save is required because the server is going to shutdown. See
       * {@link Session.Definition#preserveOnStop()}.
       */
      PRESERVE_ON_STOP;
    }

    /**
     * Default store.
     */
    Store NOOP = new Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
      }

      @Override
      public Session get(final String id) {
        return null;
      }

      @Override
      public void delete(final String id) {
      }
    };

    /**
     * Get a session by ID (if any).
     *
     * @param id Session ID.
     * @return A session or <code>null</code>.
     * @throws Exception If something goes wrong.
     */
    @Nullable
    Session get(@Nonnull String id) throws Exception;

    /**
     * Save/persist a session.
     *
     * @param session A session to be persisted.
     * @param reason An informative session of why the session need to be persisted.
     * @throws Exception If something goes wrong.
     */
    void save(@Nonnull Session session, @Nonnull SaveReason reason) throws Exception;

    /**
     * Delete a session by ID.
     *
     * @param id A session ID.
     * @throws Exception If something goes wrong.
     */
    void delete(@Nonnull String id) throws Exception;

    /**
     * Generate a session ID.
     *
     * @param seed A seed to use (if need it).
     * @return A unique session ID.
     */
    default String generateID(final long seed) {
      return UUID.randomUUID().toString();
    }
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
  @Nonnull
  <T> Optional<T> get(final @Nonnull String name);

  /**
   * @return An immutable copy of local attributes.
   */
  @Nonnull
  Map<String, Object> attributes();

  /**
   * Test if the var name exists inside the session local attributes.
   *
   * @param name A local var's name.
   * @return True, for existing locals.
   */
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
  @Nonnull Session set(final @Nonnull String name, final @Nonnull Object value);

  /**
   * Remove a local value (if any) from session locals.
   *
   * @param name A local var's name.
   * @return Existing value or empty optional.
   */
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
