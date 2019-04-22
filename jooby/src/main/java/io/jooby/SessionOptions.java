package io.jooby;

import io.jooby.internal.InMemorySessionStore;
import io.jooby.internal.RequestSessionStore;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Function;

/**
 * Options for HTTP session. Allows provides a session ID generator, session store and configure
 * cookie details, like: name, max-age, path, etc.
 *
 * Uses a memory session store, which you should combine with an a sticky sessions proxy if you
 * plan to run multiple instances.
 *
 * @author edgar
 * @since 2.0.0
 */
public class SessionOptions {
  private static final int ID_SIZE = 30;

  private static final SecureRandom secure = new SecureRandom();

  private Function<Context, String> idGenerator;

  private Cookie cookie = new Cookie("jooby.sid")
      .setMaxAge(Duration.ofSeconds(-1))
      .setHttpOnly(true)
      .setPath("/");

  private SessionStore store = new InMemorySessionStore();

  /**
   * Cookie configuration. This method returns a copy of the existing configuration.
   *
   * @return Cookie configuration. This method returns a copy of the existing configuration.
   */
  public @Nonnull Cookie getCookie() {
    return cookie.clone();
  }

  /**
   * Set/changes cookie configuration.
   *
   * @param cookie Cookie configuration.
   * @return This options.
   */
  public @Nonnull SessionOptions setCookie(@Nonnull Cookie cookie) {
    this.cookie = cookie;
    return this;
  }

  /**
   * Session store (defaults uses memory).
   *
   * @return Session store (defaults uses memory).
   */
  public @Nonnull SessionStore getStore() {
    return new RequestSessionStore(store);
  }

  /**
   * Set session store.
   *
   * @param store Session store.
   * @return This options.
   */
  public @Nonnull SessionOptions setStore(@Nonnull SessionStore store) {
    this.store = store;
    return this;
  }

  /**
   * Set custom session ID generator.
   *
   * @param idGenerator ID generator.
   * @return This options.
   */
  public SessionOptions setIdGenerator(@Nonnull Function<Context, String> idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  /**
   * Generates a Session ID.
   *
   * @param ctx Web Context.
   * @return Session ID.
   */
  public @Nonnull String generateId(@Nonnull Context ctx) {
    if (idGenerator == null) {
      byte[] bytes = new byte[ID_SIZE];
      secure.nextBytes(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    return idGenerator.apply(ctx);
  }

}
